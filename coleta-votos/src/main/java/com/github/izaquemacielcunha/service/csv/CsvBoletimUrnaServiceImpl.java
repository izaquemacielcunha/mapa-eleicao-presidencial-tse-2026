package com.github.izaquemacielcunha.service.csv;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

import com.github.izaquemacielcunha.config.EleicaoConfig;
import com.github.izaquemacielcunha.exception.EleicaoDataProcessingException;
import com.github.izaquemacielcunha.model.csv.LinhaBoletimUrna;
import com.github.izaquemacielcunha.model.urna.boletim.ResumoVotacao;

@Singleton
public class CsvBoletimUrnaServiceImpl implements CsvBoletimUrnaService {
    private static final Logger logger = LoggerFactory.getLogger(CsvBoletimUrnaServiceImpl.class);

    private final CsvMapper mapper;
    private final EleicaoConfig config;

    public CsvBoletimUrnaServiceImpl(CsvMapper mapper) {
        this(mapper, EleicaoConfig.padrao());
    }

    @Inject
    public CsvBoletimUrnaServiceImpl(CsvMapper mapper, EleicaoConfig config) {
        this.mapper = Objects.requireNonNull(mapper, "CsvMapper não pode ser nulo");
        this.config = Objects.requireNonNull(config, "EleicaoConfig não pode ser nulo");
    }

    @Override
    public void exportarVotosParaCsv(List<ResumoVotacao> resultados) {
        Objects.requireNonNull(resultados, "Resultados de votação não podem ser nulos");
        Instant inicio = Instant.now();

        List<LinhaBoletimUrna> linhasBoletins = resultados.parallelStream().map(resumoVotacao ->
                new LinhaBoletimUrna(
                        resumoVotacao.candidato(),
                        resumoVotacao.votos(),
                        resumoVotacao.boletimUrna()))
                .toList();

        CsvSchema schemaEscrita = mapper.schemaFor(LinhaBoletimUrna.class).withHeader();
        try {
            if (!Files.exists(config.diretorioTemp())) {
                Files.createDirectories(config.diretorioTemp());
            }
            mapper.writer(schemaEscrita).writeValue(config.arquivoBoletimUrnaCsv().toFile(), linhasBoletins);
        } catch (IOException | JacksonException e) {
            logger.error("Erro ao gravar CSV de votos dos boletins: {}", config.arquivoBoletimUrnaCsv(), e);
            throw new EleicaoDataProcessingException("Erro ao salvar arquivo de votos do boletim de urna: " + config.arquivoBoletimUrnaCsv(), e);
        }

        Duration decorrido = Duration.between(inicio, Instant.now());
        logger.info("CSV de boletins de urna gerado com sucesso em {} ms. Total de linhas gravadas: {}.",
                decorrido.toMillis(), linhasBoletins.size());
    }

    @Deprecated
    public void processar(List<ResumoVotacao> resultados) {
        exportarVotosParaCsv(resultados);
    }

    @Deprecated
    public void processarBoletins(List<ResumoVotacao> resultados) {
        exportarVotosParaCsv(resultados);
    }

    @Deprecated
    public void process(List<ResumoVotacao> resultados) {
        exportarVotosParaCsv(resultados);
    }
}
