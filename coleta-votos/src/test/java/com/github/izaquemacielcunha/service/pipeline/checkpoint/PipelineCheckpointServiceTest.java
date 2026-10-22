package com.github.izaquemacielcunha.service.pipeline.checkpoint;

import com.github.izaquemacielcunha.config.EleicaoConfig;
import com.github.izaquemacielcunha.dependencyinjection.MapperModule;
import com.github.izaquemacielcunha.model.pipeline.checkpoint.EstadoCheckpoint;
import com.github.izaquemacielcunha.model.pipeline.EtapaPipeline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes do Serviço de Checkpoint do Pipeline")
class PipelineCheckpointServiceTest {

    @TempDir
    Path pastaTemp;

    private EleicaoConfig config;
    private final ObjectMapper objectMapper = MapperModule.provideObjectMapper();
    private PipelineCheckpointService checkpointService;

    @BeforeEach
    void setUp() {
        Path dirTemp = pastaTemp.resolve("temp-files");
        Path dirBoletins = dirTemp.resolve("boletins-urnas");
        Path coords = pastaTemp.resolve("coords.csv");

        config = new EleicaoConfig(
                "PR",
                "75353",
                "3220",
                "ele2026",
                dirTemp,
                dirBoletins,
                coords,
                true
        );
        checkpointService = new PipelineCheckpointServiceImpl(config, objectMapper);
    }

    @Test
    @DisplayName("Deve registrar etapa concluída e persistir JSON atomicamente")
    void deveRegistrarEtapaEPersistirJson() throws IOException {
        checkpointService.prepararContextoExecucao();

        // Cria o arquivo físico que valida a etapa
        Files.createDirectories(config.diretorioTemp());
        Files.writeString(config.arquivoTempCsv(), "uf,municipio,zona,secao\nPR,75353,1,1\n");

        checkpointService.registrarEtapaConcluida(EtapaPipeline.ESTRUTURA_SECOES);

        assertTrue(Files.exists(config.arquivoCheckpointJson()));
        assertTrue(checkpointService.isEtapaConcluida(EtapaPipeline.ESTRUTURA_SECOES));

        // Recria uma nova instância do serviço simulando reinício do processo
        PipelineCheckpointService novaInstancia = new PipelineCheckpointServiceImpl(config, objectMapper);
        novaInstancia.prepararContextoExecucao();
        assertTrue(novaInstancia.isEtapaConcluida(EtapaPipeline.ESTRUTURA_SECOES));
    }

    @Test
    @DisplayName("Deve retornar false se o artefato físico não existir ou estiver vazio mesmo registrado no estado")
    void deveRetornarFalseSeArtefatoNaoExistirOuEstiverVazio() throws IOException {
        checkpointService.prepararContextoExecucao();

        // 1. Não existe arquivo físico
        checkpointService.registrarEtapaConcluida(EtapaPipeline.ESTRUTURA_SECOES);
        assertFalse(checkpointService.isEtapaConcluida(EtapaPipeline.ESTRUTURA_SECOES),
                "Deve retornar false quando arquivo físico não existe");

        // 2. Arquivo físico existe mas está vazio (0 bytes)
        Files.createDirectories(config.diretorioTemp());
        Files.write(config.arquivoTempCsv(), new byte[0]);
        assertFalse(checkpointService.isEtapaConcluida(EtapaPipeline.ESTRUTURA_SECOES),
                "Deve retornar false quando arquivo físico está com tamanho zero");

        // 3. Arquivo físico existe e tem conteúdo (> 0 bytes)
        Files.writeString(config.arquivoTempCsv(), "conteudo-valido");
        assertTrue(checkpointService.isEtapaConcluida(EtapaPipeline.ESTRUTURA_SECOES),
                "Deve retornar true quando arquivo físico tem conteúdo");
    }

    @Test
    @DisplayName("Deve validar corretamente todas as etapas com seus respectivos artefatos físicos")
    void deveValidarTodasEtapasComSeusArtefatos() throws IOException {
        Files.createDirectories(config.diretorioTemp());

        // ESTRUTURA_SECOES -> arquivoTempCsv
        Files.writeString(config.arquivoTempCsv(), "csv");
        checkpointService.registrarEtapaConcluida(EtapaPipeline.ESTRUTURA_SECOES);
        assertTrue(checkpointService.isEtapaConcluida(EtapaPipeline.ESTRUTURA_SECOES));

        // ENRIQUECIMENTO_METADADOS -> arquivoEnriquecidoCsv
        Files.writeString(config.arquivoEnriquecidoCsv(), "csv");
        checkpointService.registrarEtapaConcluida(EtapaPipeline.ENRIQUECIMENTO_METADADOS);
        assertTrue(checkpointService.isEtapaConcluida(EtapaPipeline.ENRIQUECIMENTO_METADADOS));

        // DOWNLOAD_BOLETINS -> diretorioBoletins
        Files.createDirectories(config.diretorioBoletins());
        checkpointService.registrarEtapaConcluida(EtapaPipeline.DOWNLOAD_BOLETINS);
        assertTrue(checkpointService.isEtapaConcluida(EtapaPipeline.DOWNLOAD_BOLETINS));

        // APURACAO_VOTOS -> arquivoBoletimUrnaCsv
        Files.writeString(config.arquivoBoletimUrnaCsv(), "csv");
        checkpointService.registrarEtapaConcluida(EtapaPipeline.APURACAO_VOTOS);
        assertTrue(checkpointService.isEtapaConcluida(EtapaPipeline.APURACAO_VOTOS));

        // MESCLAGEM_VOTOS -> arquivoResultadoMescladoCsv
        Files.writeString(config.arquivoResultadoMescladoCsv(), "csv");
        checkpointService.registrarEtapaConcluida(EtapaPipeline.MESCLAGEM_VOTOS);
        assertTrue(checkpointService.isEtapaConcluida(EtapaPipeline.MESCLAGEM_VOTOS));

        // EXPORTACAO_CARTO -> arquivoCartoFinal
        Files.writeString(config.arquivoCartoFinal(), "csv");
        try {
            checkpointService.registrarEtapaConcluida(EtapaPipeline.EXPORTACAO_CARTO);
            assertTrue(checkpointService.isEtapaConcluida(EtapaPipeline.EXPORTACAO_CARTO));
        } finally {
            Files.deleteIfExists(config.arquivoCartoFinal());
        }
    }

    @Test
    @DisplayName("Deve invalidar checkpoint anterior se o contexto (município/UF) mudou")
    void deveInvalidarCheckpointSeContextoMudou() throws IOException {
        // Grava manualmente um checkpoint de outro município (75000)
        Files.createDirectories(config.diretorioTemp());
        EstadoCheckpoint estadoAnterior = new EstadoCheckpoint(
                "PR",
                "75000",
                "3220",
                "ele2026",
                Set.of(EtapaPipeline.ESTRUTURA_SECOES),
                Instant.now()
        );
        Files.write(config.arquivoCheckpointJson(), objectMapper.writeValueAsBytes(estadoAnterior));

        checkpointService.prepararContextoExecucao();

        // O checkpoint de 75000 deve ter sido descartado, logo ESTRUTURA_SECOES não deve estar concluída para 75353
        assertFalse(checkpointService.isEtapaConcluida(EtapaPipeline.ESTRUTURA_SECOES));
        assertFalse(Files.exists(config.arquivoCheckpointJson()));
    }

    @Test
    @DisplayName("Deve limpar arquivo e estado ao chamar limparCheckpoint")
    void deveLimparCheckpoint() throws IOException {
        Files.createDirectories(config.diretorioTemp());
        Files.writeString(config.arquivoTempCsv(), "dados");
        checkpointService.registrarEtapaConcluida(EtapaPipeline.ESTRUTURA_SECOES);
        assertTrue(Files.exists(config.arquivoCheckpointJson()));

        checkpointService.limparCheckpoint();

        assertFalse(Files.exists(config.arquivoCheckpointJson()));
        assertFalse(checkpointService.isEtapaConcluida(EtapaPipeline.ESTRUTURA_SECOES));
    }
}
