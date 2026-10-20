package com.github.izaquemacielcunha.service.checkpoint;

import com.github.izaquemacielcunha.config.EleicaoConfig;
import com.github.izaquemacielcunha.exception.EleicaoDataProcessingException;
import com.github.izaquemacielcunha.model.checkpoint.EstadoCheckpoint;
import com.github.izaquemacielcunha.model.checkpoint.EtapaPipeline;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

@Singleton
public class PipelineCheckpointServiceImpl implements PipelineCheckpointService {

    private static final Logger logger = LoggerFactory.getLogger(PipelineCheckpointServiceImpl.class);

    private final EleicaoConfig config;
    private final ObjectMapper objectMapper;
    private EstadoCheckpoint estadoAtual;

    @Inject
    public PipelineCheckpointServiceImpl(EleicaoConfig config, ObjectMapper objectMapper) {
        this.config = Objects.requireNonNull(config, "Configuração não pode ser nula");
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper não pode ser nulo");
    }

    @Override
    public void prepararContextoExecucao() {
        try {
            Files.createDirectories(config.diretorioTemp());
        } catch (IOException e) {
            throw new EleicaoDataProcessingException("Falha ao criar diretório temporário para checkpoint: " + config.diretorioTemp(), e);
        }

        Path arquivoCheckpoint = config.arquivoCheckpointJson();
        if (Files.exists(arquivoCheckpoint)) {
            try (InputStream is = Files.newInputStream(arquivoCheckpoint)) {
                EstadoCheckpoint estadoLido = objectMapper.readValue(is, EstadoCheckpoint.class);
                if (estadoLido != null && estadoLido.pertenceAoContexto(config)) {
                    this.estadoAtual = estadoLido;
                    logger.info("[Checkpoint] Contexto recuperado com sucesso para {}/{}: {} etapas concluídas.",
                            config.uf(), config.codigoMunicipio(), estadoAtual.etapasConcluidas().size());
                    return;
                } else {
                    logger.warn("[Checkpoint] Checkpoint existente pertence a outro contexto ({}/{}). Invalidando checkpoint anterior...",
                            estadoLido != null ? estadoLido.uf() : "desconhecido",
                            estadoLido != null ? estadoLido.codigoMunicipio() : "desconhecido");
                    limparCheckpoint();
                }
            } catch (Exception e) {
                logger.warn("[Checkpoint] Erro ao ler arquivo de checkpoint existente ({}). Reinicializando do zero...", e.getMessage());
                limparCheckpoint();
            }
        }

        this.estadoAtual = EstadoCheckpoint.inicial(config);
    }

    @Override
    public boolean isEtapaConcluida(EtapaPipeline etapa) {
        Objects.requireNonNull(etapa, "Etapa não pode ser nula");
        if (estadoAtual == null) {
            prepararContextoExecucao();
        }

        if (!estadoAtual.etapasConcluidas().contains(etapa)) {
            return false;
        }

        boolean artefatoValido = validarArtefatoFisico(etapa);
        if (!artefatoValido) {
            logger.warn("[Checkpoint] Etapa {} está marcada como concluída no estado, mas o artefato físico esperado não existe ou está vazio. Forçando reexecução.", etapa);
            return false;
        }

        return true;
    }

    @Override
    public void registrarEtapaConcluida(EtapaPipeline etapa) {
        Objects.requireNonNull(etapa, "Etapa não pode ser nula");
        if (estadoAtual == null) {
            prepararContextoExecucao();
        }

        this.estadoAtual = this.estadoAtual.comEtapaConcluida(etapa);
        salvarEstadoAtomicamente();
        logger.info("[Checkpoint] Etapa {} ({}) gravada com sucesso no checkpoint.", etapa, etapa.descricao());
    }

    @Override
    public void limparCheckpoint() {
        Path arquivo = config.arquivoCheckpointJson();
        Path arquivoTmp = config.diretorioTemp().resolve("checkpoint-status.json.tmp");
        try {
            Files.deleteIfExists(arquivo);
            Files.deleteIfExists(arquivoTmp);
            this.estadoAtual = EstadoCheckpoint.inicial(config);
            logger.info("[Checkpoint] Arquivo de checkpoint removido e estado reiniciado.");
        } catch (IOException e) {
            logger.warn("[Checkpoint] Falha ao remover arquivo de checkpoint {}: {}", arquivo, e.getMessage());
        }
    }

    private boolean validarArtefatoFisico(EtapaPipeline etapa) {
        return switch (etapa) {
            case ESTRUTURA_SECOES -> validarArquivoNaoVazio(config.arquivoTempCsv());
            case ENRIQUECIMENTO_METADADOS -> validarArquivoNaoVazio(config.arquivoEnriquecidoCsv());
            case DOWNLOAD_BOLETINS -> validarDiretorioBoletins(config.diretorioBoletins());
            case APURACAO_VOTOS -> validarArquivoNaoVazio(config.arquivoBoletimUrnaCsv());
            case MESCLAGEM_VOTOS -> validarArquivoNaoVazio(config.arquivoResultadoMescladoCsv());
            case EXPORTACAO_CARTO -> validarArquivoNaoVazio(config.arquivoCartoFinal());
        };
    }

    private boolean validarArquivoNaoVazio(Path caminho) {
        try {
            return caminho != null && Files.exists(caminho) && Files.isRegularFile(caminho) && Files.size(caminho) > 0;
        } catch (IOException e) {
            logger.warn("[Checkpoint] Erro ao verificar tamanho do arquivo {}: {}", caminho, e.getMessage());
            return false;
        }
    }

    private boolean validarDiretorioBoletins(Path caminho) {
        return caminho != null && Files.exists(caminho) && Files.isDirectory(caminho);
    }

    private void salvarEstadoAtomicamente() {
        Path arquivoDestino = config.arquivoCheckpointJson();
        Path arquivoTmp = config.diretorioTemp().resolve("checkpoint-status.json.tmp");
        try {
            Files.createDirectories(config.diretorioTemp());
            byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(estadoAtual);
            Files.write(arquivoTmp, bytes);
            try {
                Files.move(arquivoTmp, arquivoDestino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(arquivoTmp, arquivoDestino, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new EleicaoDataProcessingException("Falha ao salvar o checkpoint atomicamente em " + arquivoDestino, e);
        }
    }
}
