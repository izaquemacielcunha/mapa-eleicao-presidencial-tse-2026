package com.github.izaquemacielcunha.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;

import com.github.izaquemacielcunha.config.EleicaoConfig;
import com.github.izaquemacielcunha.model.Municipio;
import com.github.izaquemacielcunha.model.urna.boletim.ResumoVotacao;
import com.github.izaquemacielcunha.service.csv.CsvBoletimUrnaService;
import com.github.izaquemacielcunha.service.csv.CsvCartoExportadorService;
import com.github.izaquemacielcunha.service.csv.CsvEnriquecedorService;
import com.github.izaquemacielcunha.service.csv.CsvMescladorBoletimService;
import com.github.izaquemacielcunha.service.csv.CsvSecaoService;
import com.github.izaquemacielcunha.service.tse.TseUrlBuilder;
import com.github.izaquemacielcunha.service.urna.boletim.BoletimUrnaDownloadService;
import com.github.izaquemacielcunha.service.urna.boletim.BoletimUrnaService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class EleicaoPipelineServiceTest {

    private ExternalService externalService;
    private TseUrlBuilder tseUrlBuilder;
    private CsvSecaoService csvSecaoService;
    private CsvEnriquecedorService csvEnriquecedorService;
    private BoletimUrnaDownloadService boletimUrnaDownloader;
    private BoletimUrnaService boletimUrnaService;
    private CsvBoletimUrnaService csvBoletimUrnaService;
    private CsvMescladorBoletimService csvMescladorBoletimService;
    private CsvCartoExportadorService csvCartoExportadorService;
    private LimpezaArquivosTemporariosService limpezaArquivosTemporariosService;

    @BeforeEach
    void setUp() {
        externalService = mock(ExternalService.class);
        tseUrlBuilder = mock(TseUrlBuilder.class);
        csvSecaoService = mock(CsvSecaoService.class);
        csvEnriquecedorService = mock(CsvEnriquecedorService.class);
        boletimUrnaDownloader = mock(BoletimUrnaDownloadService.class);
        boletimUrnaService = mock(BoletimUrnaService.class);
        csvBoletimUrnaService = mock(CsvBoletimUrnaService.class);
        csvMescladorBoletimService = mock(CsvMescladorBoletimService.class);
        csvCartoExportadorService = mock(CsvCartoExportadorService.class);
        limpezaArquivosTemporariosService = mock(LimpezaArquivosTemporariosService.class);
    }

    @Test
    @DisplayName("executar deve aplicar Fail-Fast quando o município não for encontrado no TSE")
    void deveAplicarFailFastQuandoMunicipioNaoEncontrado(@TempDir Path tempDir) throws Exception {
        Path arquivoCoords = tempDir.resolve("coords-tse.csv");
        Files.createFile(arquivoCoords);

        EleicaoConfig config = new EleicaoConfig(
                "PR",
                "99999",
                "3220",
                "ele2026",
                tempDir,
                tempDir.resolve("boletins"),
                arquivoCoords
        );

        when(tseUrlBuilder.buildUrlMunicipioConfig()).thenReturn("https://tse.jus.br/config.json");
        when(externalService.buscarConfiguracaoMunicipio(anyString(), anyString())).thenReturn(Optional.empty());

        EleicaoPipelineService pipeline = new EleicaoPipelineServiceImpl(
                config,
                externalService,
                tseUrlBuilder,
                csvSecaoService,
                csvEnriquecedorService,
                boletimUrnaDownloader,
                boletimUrnaService,
                csvBoletimUrnaService,
                csvMescladorBoletimService,
                csvCartoExportadorService,
                limpezaArquivosTemporariosService
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, pipeline::executar);
        assertTrue(ex.getMessage().contains("Município não encontrado para o código: 99999"));

        // Garante que fases posteriores não foram executadas
        verify(csvEnriquecedorService, never()).enriquecerMetadadosSecoes();
        verify(boletimUrnaDownloader, never()).baixarBoletinsUrna();
        verify(limpezaArquivosTemporariosService, never()).limparArquivosTemporarios();
    }

    @Test
    @DisplayName("executar deve orquestrar sequencialmente as etapas do pipeline com sucesso incluindo limpeza final")
    void deveOrquestrarComSucessoTodasAsFases(@TempDir Path tempDir) throws Exception {
        Path arquivoCoords = tempDir.resolve("coords-tse.csv");
        Files.createFile(arquivoCoords);

        EleicaoConfig config = new EleicaoConfig(
                "PR",
                "75353",
                "3220",
                "ele2026",
                tempDir,
                tempDir.resolve("boletins"),
                arquivoCoords
        );

        Municipio municipio = new Municipio("75353", "CURITIBA", List.of());
        when(tseUrlBuilder.buildUrlMunicipioConfig()).thenReturn("https://tse.jus.br/config.json");
        when(externalService.buscarConfiguracaoMunicipio(anyString(), eq("75353"))).thenReturn(Optional.of(municipio));

        List<ResumoVotacao> resumoVotacao = List.of(new ResumoVotacao("13", 150, "bu-1.dat"));
        when(boletimUrnaService.processarDiretorio(any(Path.class))).thenReturn(resumoVotacao);

        EleicaoPipelineService pipeline = new EleicaoPipelineServiceImpl(
                config,
                externalService,
                tseUrlBuilder,
                csvSecaoService,
                csvEnriquecedorService,
                boletimUrnaDownloader,
                boletimUrnaService,
                csvBoletimUrnaService,
                csvMescladorBoletimService,
                csvCartoExportadorService,
                limpezaArquivosTemporariosService
        );

        pipeline.executar();

        // Verifica a ordem estrita de execução das fases
        InOrder inOrder = inOrder(
                externalService,
                csvSecaoService,
                csvEnriquecedorService,
                boletimUrnaDownloader,
                boletimUrnaService,
                csvBoletimUrnaService,
                csvMescladorBoletimService,
                csvCartoExportadorService,
                limpezaArquivosTemporariosService
        );

        inOrder.verify(externalService).buscarConfiguracaoMunicipio(anyString(), eq("75353"));
        inOrder.verify(csvSecaoService).exportarSecoesMunicipaisParaCsv(any());
        inOrder.verify(csvEnriquecedorService).enriquecerMetadadosSecoes();
        inOrder.verify(boletimUrnaDownloader).baixarBoletinsUrna();
        inOrder.verify(boletimUrnaService).processarDiretorio(config.diretorioBoletins());
        inOrder.verify(csvBoletimUrnaService).exportarVotosParaCsv(resumoVotacao);
        inOrder.verify(csvMescladorBoletimService).mesclarBoletinsComMunicipios();
        inOrder.verify(csvCartoExportadorService).exportarDatasetCarto(
                eq(config.arquivoCoordenadasTse()),
                eq(config.arquivoResultadoMescladoCsv()),
                eq(config.arquivoCartoFinal()),
                eq(config.codigoMunicipio())
        );
        inOrder.verify(limpezaArquivosTemporariosService).limparArquivosTemporarios();
    }
}
