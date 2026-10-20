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
import com.github.izaquemacielcunha.model.checkpoint.EtapaPipeline;
import com.github.izaquemacielcunha.model.urna.boletim.ResumoVotacao;
import com.github.izaquemacielcunha.service.checkpoint.PipelineCheckpointService;
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
    private PipelineCheckpointService checkpointService;

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
        checkpointService = mock(PipelineCheckpointService.class);
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
                limpezaArquivosTemporariosService,
                checkpointService
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, pipeline::executar);
        assertTrue(ex.getMessage().contains("Município não encontrado para o código: 99999"));

        verify(checkpointService).prepararContextoExecucao();
        verify(checkpointService, never()).registrarEtapaConcluida(any());
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
                limpezaArquivosTemporariosService,
                checkpointService
        );

        pipeline.executar();

        // Verifica a ordem estrita de execução das fases e registros de checkpoint
        InOrder inOrder = inOrder(
                checkpointService,
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

        inOrder.verify(checkpointService).prepararContextoExecucao();

        inOrder.verify(checkpointService).isEtapaConcluida(EtapaPipeline.ESTRUTURA_SECOES);
        inOrder.verify(externalService).buscarConfiguracaoMunicipio(anyString(), eq("75353"));
        inOrder.verify(csvSecaoService).exportarSecoesMunicipaisParaCsv(any());
        inOrder.verify(checkpointService).registrarEtapaConcluida(EtapaPipeline.ESTRUTURA_SECOES);

        inOrder.verify(checkpointService).isEtapaConcluida(EtapaPipeline.ENRIQUECIMENTO_METADADOS);
        inOrder.verify(csvEnriquecedorService).enriquecerMetadadosSecoes();
        inOrder.verify(checkpointService).registrarEtapaConcluida(EtapaPipeline.ENRIQUECIMENTO_METADADOS);

        inOrder.verify(checkpointService).isEtapaConcluida(EtapaPipeline.DOWNLOAD_BOLETINS);
        inOrder.verify(boletimUrnaDownloader).baixarBoletinsUrna();
        inOrder.verify(checkpointService).registrarEtapaConcluida(EtapaPipeline.DOWNLOAD_BOLETINS);

        inOrder.verify(checkpointService).isEtapaConcluida(EtapaPipeline.APURACAO_VOTOS);
        inOrder.verify(boletimUrnaService).processarDiretorio(config.diretorioBoletins());
        inOrder.verify(csvBoletimUrnaService).exportarVotosParaCsv(resumoVotacao);
        inOrder.verify(checkpointService).registrarEtapaConcluida(EtapaPipeline.APURACAO_VOTOS);

        inOrder.verify(checkpointService).isEtapaConcluida(EtapaPipeline.MESCLAGEM_VOTOS);
        inOrder.verify(csvMescladorBoletimService).mesclarBoletinsComMunicipios();
        inOrder.verify(checkpointService).registrarEtapaConcluida(EtapaPipeline.MESCLAGEM_VOTOS);

        inOrder.verify(checkpointService).isEtapaConcluida(EtapaPipeline.EXPORTACAO_CARTO);
        inOrder.verify(csvCartoExportadorService).exportarDatasetCarto(
                eq(config.arquivoCoordenadasTse()),
                eq(config.arquivoResultadoMescladoCsv()),
                eq(config.arquivoCartoFinal()),
                eq(config.codigoMunicipio())
        );
        inOrder.verify(checkpointService).registrarEtapaConcluida(EtapaPipeline.EXPORTACAO_CARTO);

        inOrder.verify(limpezaArquivosTemporariosService).limparArquivosTemporarios();
    }

    @Test
    @DisplayName("executar deve retomar pipeline pulando etapas já concluídas no checkpoint")
    void deveRetomarExecucaoPulandoEtapasJaConcluidasNoCheckpoint(@TempDir Path tempDir) throws Exception {
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

        // Simula que Fases 1, 2 e 3 já estão concluídas e com artefatos válidos
        when(checkpointService.isEtapaConcluida(EtapaPipeline.ESTRUTURA_SECOES)).thenReturn(true);
        when(checkpointService.isEtapaConcluida(EtapaPipeline.ENRIQUECIMENTO_METADADOS)).thenReturn(true);
        when(checkpointService.isEtapaConcluida(EtapaPipeline.DOWNLOAD_BOLETINS)).thenReturn(true);

        // Fases 4, 5 e 6 pendentes
        when(checkpointService.isEtapaConcluida(EtapaPipeline.APURACAO_VOTOS)).thenReturn(false);
        when(checkpointService.isEtapaConcluida(EtapaPipeline.MESCLAGEM_VOTOS)).thenReturn(false);
        when(checkpointService.isEtapaConcluida(EtapaPipeline.EXPORTACAO_CARTO)).thenReturn(false);

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
                limpezaArquivosTemporariosService,
                checkpointService
        );

        pipeline.executar();

        // Verifica que as fases 1, 2 e 3 foram PULADAS (nunca chamadas)
        verify(externalService, never()).buscarConfiguracaoMunicipio(anyString(), anyString());
        verify(csvSecaoService, never()).exportarSecoesMunicipaisParaCsv(any());
        verify(csvEnriquecedorService, never()).enriquecerMetadadosSecoes();
        verify(boletimUrnaDownloader, never()).baixarBoletinsUrna();

        // E não registraram novamente conclusão
        verify(checkpointService, never()).registrarEtapaConcluida(EtapaPipeline.ESTRUTURA_SECOES);
        verify(checkpointService, never()).registrarEtapaConcluida(EtapaPipeline.ENRIQUECIMENTO_METADADOS);
        verify(checkpointService, never()).registrarEtapaConcluida(EtapaPipeline.DOWNLOAD_BOLETINS);

        // Verifica que as fases 4, 5 e 6 foram EXECUTADAS e registradas
        verify(boletimUrnaService).processarDiretorio(config.diretorioBoletins());
        verify(csvBoletimUrnaService).exportarVotosParaCsv(resumoVotacao);
        verify(checkpointService).registrarEtapaConcluida(EtapaPipeline.APURACAO_VOTOS);

        verify(csvMescladorBoletimService).mesclarBoletinsComMunicipios();
        verify(checkpointService).registrarEtapaConcluida(EtapaPipeline.MESCLAGEM_VOTOS);

        verify(csvCartoExportadorService).exportarDatasetCarto(
                eq(config.arquivoCoordenadasTse()),
                eq(config.arquivoResultadoMescladoCsv()),
                eq(config.arquivoCartoFinal()),
                eq(config.codigoMunicipio())
        );
        verify(checkpointService).registrarEtapaConcluida(EtapaPipeline.EXPORTACAO_CARTO);

        verify(limpezaArquivosTemporariosService).limparArquivosTemporarios();
    }

    @Test
    @DisplayName("executar deve interromper quando ocorrer falha e manter progresso anterior salvo")
    void deveInterromperPipelineMantendoProgressoSalvoQuandoOcorrerErro(@TempDir Path tempDir) throws Exception {
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

        // Fase 1 e 2 passam, Fase 3 falha
        doThrow(new RuntimeException("Timeout de rede no TSE")).when(boletimUrnaDownloader).baixarBoletinsUrna();

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
                limpezaArquivosTemporariosService,
                checkpointService
        );

        RuntimeException ex = assertThrows(RuntimeException.class, pipeline::executar);
        assertTrue(ex.getMessage().contains("Timeout de rede no TSE"));

        // Fases 1 e 2 foram registradas no checkpoint antes da quebra
        verify(checkpointService).registrarEtapaConcluida(EtapaPipeline.ESTRUTURA_SECOES);
        verify(checkpointService).registrarEtapaConcluida(EtapaPipeline.ENRIQUECIMENTO_METADADOS);

        // Fase 3 falhou: NÃO deve ter sido registrada como concluída
        verify(checkpointService, never()).registrarEtapaConcluida(EtapaPipeline.DOWNLOAD_BOLETINS);

        // Fases subsequentes NÃO foram executadas nem registradas
        verify(boletimUrnaService, never()).processarDiretorio(any());
        verify(checkpointService, never()).registrarEtapaConcluida(EtapaPipeline.APURACAO_VOTOS);
        verify(csvMescladorBoletimService, never()).mesclarBoletinsComMunicipios();
        verify(checkpointService, never()).registrarEtapaConcluida(EtapaPipeline.MESCLAGEM_VOTOS);
        verify(csvCartoExportadorService, never()).exportarDatasetCarto(any(), any(), any(), any());
        verify(checkpointService, never()).registrarEtapaConcluida(EtapaPipeline.EXPORTACAO_CARTO);
        verify(limpezaArquivosTemporariosService, never()).limparArquivosTemporarios();
    }
}
