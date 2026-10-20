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
import com.github.izaquemacielcunha.service.csv.CsvService;
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
    private CsvService csvService;
    private CsvEnriquecedorService csvEnriquecedorService;
    private BoletimUrnaDownloadService boletimUrnaDownloader;
    private BoletimUrnaService boletimUrnaService;
    private CsvBoletimUrnaService csvBoletimUrnaService;
    private CsvMescladorBoletimService csvMescladorBoletimService;
    private CsvCartoExportadorService csvCartoExportadorService;

    @BeforeEach
    void setUp() {
        externalService = mock(ExternalService.class);
        tseUrlBuilder = mock(TseUrlBuilder.class);
        csvService = mock(CsvService.class);
        csvEnriquecedorService = mock(CsvEnriquecedorService.class);
        boletimUrnaDownloader = mock(BoletimUrnaDownloadService.class);
        boletimUrnaService = mock(BoletimUrnaService.class);
        csvBoletimUrnaService = mock(CsvBoletimUrnaService.class);
        csvMescladorBoletimService = mock(CsvMescladorBoletimService.class);
        csvCartoExportadorService = mock(CsvCartoExportadorService.class);
    }

    @Test
    @DisplayName("executar deve aplicar Fail-Fast quando o município não for encontrado no TSE")
    void deveAplicarFailFastQuandoMunicipioNaoEncontrado(@TempDir Path tempDir) throws Exception {
        EleicaoConfig config = new EleicaoConfig(
                "PR",
                "99999",
                "3220",
                "ele2026",
                tempDir,
                tempDir.resolve("boletins")
        );
        // Cria arquivo de coordenadas para passar na primeira validação
        Files.createFile(config.arquivoCoordenadasTse());

        when(tseUrlBuilder.buildUrlMunicipioConfig()).thenReturn("https://tse.jus.br/config.json");
        when(externalService.buscarConfiguracaoMunicipio(anyString(), anyString())).thenReturn(Optional.empty());

        EleicaoPipelineService pipeline = new EleicaoPipelineService(
                config,
                externalService,
                tseUrlBuilder,
                csvService,
                csvEnriquecedorService,
                boletimUrnaDownloader,
                boletimUrnaService,
                csvBoletimUrnaService,
                csvMescladorBoletimService,
                csvCartoExportadorService
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, pipeline::executar);
        assertTrue(ex.getMessage().contains("Município não encontrado para o código: 99999"));

        // Garante que fases posteriores não foram executadas
        verify(csvEnriquecedorService, never()).enriquecer();
        verify(boletimUrnaDownloader, never()).baixarBoletins();
    }

    @Test
    @DisplayName("executar deve orquestrar sequencialmente as 6 fases do pipeline com sucesso")
    void deveOrquestrarComSucessoTodasAsFases(@TempDir Path tempDir) throws Exception {
        EleicaoConfig config = new EleicaoConfig(
                "PR",
                "75353",
                "3220",
                "ele2026",
                tempDir,
                tempDir.resolve("boletins")
        );
        Files.createFile(config.arquivoCoordenadasTse());

        Municipio municipio = new Municipio("75353", "CURITIBA", List.of());
        when(tseUrlBuilder.buildUrlMunicipioConfig()).thenReturn("https://tse.jus.br/config.json");
        when(externalService.buscarConfiguracaoMunicipio(anyString(), eq("75353"))).thenReturn(Optional.of(municipio));

        when(csvEnriquecedorService.enriquecer()).thenReturn(new CsvEnriquecedorService.ResultadoEnriquecimento(10, 8, 2));
        when(boletimUrnaDownloader.baixarBoletins()).thenReturn(new BoletimUrnaDownloadService.ResultadoDownload(10, 8, 2));
        List<ResumoVotacao> resumoVotacao = List.of(new ResumoVotacao("13", 150, "bu-1.dat"));
        when(boletimUrnaService.processarBoletins(any(Path.class))).thenReturn(resumoVotacao);
        when(csvMescladorBoletimService.mesclar()).thenReturn(10);
        when(csvCartoExportadorService.exportar(any(Path.class), any(Path.class), any(Path.class), anyString()))
                .thenReturn(new CsvCartoExportadorService.ResultadoExportacaoCarto(10, 0));

        EleicaoPipelineService pipeline = new EleicaoPipelineService(
                config,
                externalService,
                tseUrlBuilder,
                csvService,
                csvEnriquecedorService,
                boletimUrnaDownloader,
                boletimUrnaService,
                csvBoletimUrnaService,
                csvMescladorBoletimService,
                csvCartoExportadorService
        );

        pipeline.executar();

        // Verifica a ordem estrita de execução das fases
        InOrder inOrder = inOrder(
                externalService,
                csvService,
                csvEnriquecedorService,
                boletimUrnaDownloader,
                boletimUrnaService,
                csvBoletimUrnaService,
                csvMescladorBoletimService,
                csvCartoExportadorService
        );

        inOrder.verify(externalService).buscarConfiguracaoMunicipio(anyString(), eq("75353"));
        inOrder.verify(csvService).gerarCsv(any());
        inOrder.verify(csvEnriquecedorService).enriquecer();
        inOrder.verify(boletimUrnaDownloader).baixarBoletins();
        inOrder.verify(boletimUrnaService).processarBoletins(config.diretorioBoletins());
        inOrder.verify(csvBoletimUrnaService).processar(resumoVotacao);
        inOrder.verify(csvMescladorBoletimService).mesclar();
        inOrder.verify(csvCartoExportadorService).exportar(
                eq(config.arquivoCoordenadasTse()),
                eq(config.arquivoResultadoMescladoCsv()),
                eq(config.arquivoCartoFinal()),
                eq(config.codigoMunicipio())
        );
    }
}
