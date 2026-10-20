package com.github.izaquemacielcunha.service.csv;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import com.github.izaquemacielcunha.model.Abrangencia;
import com.github.izaquemacielcunha.model.csv.LinhaMunicipio;

@Singleton
public class CsvSecaoServiceImpl implements CsvSecaoService {
    private static final Logger logger = LoggerFactory.getLogger(CsvSecaoServiceImpl.class);

    private final CsvMapper mapper;
    private final EleicaoConfig config;

    public CsvSecaoServiceImpl(CsvMapper mapper) {
        this(mapper, EleicaoConfig.padrao());
    }

    @Inject
    public CsvSecaoServiceImpl(CsvMapper mapper, EleicaoConfig config) {
        this.mapper = Objects.requireNonNull(mapper, "CsvMapper não pode ser nulo");
        this.config = Objects.requireNonNull(config, "EleicaoConfig não pode ser nulo");
    }

    @Override
    public void exportarSecoesMunicipaisParaCsv(List<Abrangencia> abrangencias) {
        Objects.requireNonNull(abrangencias, "Lista de abrangências não pode ser nula");
        Instant inicio = Instant.now();

        List<LinhaMunicipio> linhas = converterParaLinhas(abrangencias);

        CsvSchema schema = mapper.schemaFor(LinhaMunicipio.class).withHeader();

        Path diretorioTemp = config.diretorioTemp();
        try {
            if (!Files.exists(diretorioTemp)) {
                Files.createDirectories(diretorioTemp);
            }
            mapper.writer(schema).writeValue(config.arquivoTempCsv().toFile(), linhas);
        } catch (IOException | JacksonException e) {
            logger.error("Erro ao gerar arquivo CSV inicial de municípios: {}", config.arquivoTempCsv(), e);
            throw new EleicaoDataProcessingException("Erro ao gerar arquivo CSV inicial de municípios: " + config.arquivoTempCsv(), e);
        }

        Duration decorrido = Duration.between(inicio, Instant.now());
        logger.info("CSV inicial gerado com sucesso em {} ms. Total de seções mapeadas: {}.",
                decorrido.toMillis(), linhas.size());
    }

    private List<LinhaMunicipio> converterParaLinhas(List<Abrangencia> abrangencias) {
        return abrangencias.stream()
                .flatMap(abrangencia -> abrangencia.municipio().zonas().stream()
                        .flatMap(zona -> zona.secoes().stream()
                                .map(secao -> new LinhaMunicipio(
                                        abrangencia.unidadeFederativa(),
                                        abrangencia.municipio().codigo(),
                                        abrangencia.municipio().nome(),
                                        zona.codigo(),
                                        secao.numero()
                                ))
                        )
                )
                .toList();
    }
}
