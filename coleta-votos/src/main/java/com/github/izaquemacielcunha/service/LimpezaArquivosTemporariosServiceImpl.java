package com.github.izaquemacielcunha.service;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.izaquemacielcunha.config.EleicaoConfig;
import com.github.izaquemacielcunha.exception.EleicaoDataProcessingException;

@Singleton
public class LimpezaArquivosTemporariosServiceImpl implements LimpezaArquivosTemporariosService {
    private static final Logger logger = LoggerFactory.getLogger(LimpezaArquivosTemporariosServiceImpl.class);

    private final EleicaoConfig config;

    @Inject
    public LimpezaArquivosTemporariosServiceImpl(EleicaoConfig config) {
        this.config = Objects.requireNonNull(config, "EleicaoConfig não pode ser nulo");
    }

    @Override
    public void limparArquivosTemporarios() {
        if (!config.limparArquivosTemporarios()) {
            logger.info("Limpeza de arquivos temporários ignorada por configuração.");
            return;
        }

        Path diretorioTemp = config.diretorioTemp().toAbsolutePath().normalize();
        if (!Files.exists(diretorioTemp)) {
            return;
        }

        validarDiretorioSeguroParaExclusao(diretorioTemp);

        Instant inicio = Instant.now();
        AtomicInteger totalItensRemovidos = new AtomicInteger();

        try {
            Files.walkFileTree(diretorioTemp, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (Files.deleteIfExists(file)) {
                        totalItensRemovidos.incrementAndGet();
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc != null) {
                        throw exc;
                    }
                    if (Files.deleteIfExists(dir)) {
                        totalItensRemovidos.incrementAndGet();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            logger.error("Erro ao limpar diretório temporário: {}", diretorioTemp, e);
            throw new EleicaoDataProcessingException("Erro ao limpar diretório temporário: " + diretorioTemp, e);
        }

        Duration decorrido = Duration.between(inicio, Instant.now());
        logger.info(
                "Limpeza do diretório temporário '{}' concluída em {} ms. Total de itens removidos: {}.",
                diretorioTemp,
                decorrido.toMillis(),
                totalItensRemovidos.get()
        );
    }

    private void validarDiretorioSeguroParaExclusao(Path diretorioNormalizado) {
        if (diretorioNormalizado.getParent() == null || diretorioNormalizado.equals(diretorioNormalizado.getRoot())) {
            throw new IllegalArgumentException("Diretório não seguro para exclusão: não é permitido excluir a raiz do sistema: " + diretorioNormalizado);
        }

        Path diretorioTrabalho = Path.of("").toAbsolutePath().normalize();
        if (diretorioNormalizado.equals(diretorioTrabalho)) {
            throw new IllegalArgumentException("Diretório não seguro para exclusão: não é permitido excluir o diretório de trabalho atual: " + diretorioNormalizado);
        }

        Path arquivoCoordenadas = config.arquivoCoordenadasTse().toAbsolutePath().normalize();
        if (arquivoCoordenadas.startsWith(diretorioNormalizado)) {
            throw new IllegalArgumentException("Diretório contém o arquivo de coordenadas do TSE e não pode ser excluído: " + diretorioNormalizado);
        }

        Path arquivoCarto = config.arquivoCartoFinal().toAbsolutePath().normalize();
        if (arquivoCarto.startsWith(diretorioNormalizado)) {
            throw new IllegalArgumentException("Diretório contém o arquivo final do Carto e não pode ser excluído: " + diretorioNormalizado);
        }
    }
}
