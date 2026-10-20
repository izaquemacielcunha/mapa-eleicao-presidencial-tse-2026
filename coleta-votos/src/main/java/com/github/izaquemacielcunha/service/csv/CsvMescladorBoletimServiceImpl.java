package com.github.izaquemacielcunha.service.csv;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.SequenceWriter;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

import com.github.izaquemacielcunha.config.EleicaoConfig;
import com.github.izaquemacielcunha.exception.EleicaoDataProcessingException;
import com.github.izaquemacielcunha.model.csv.LinhaBoletimUrna;
import com.github.izaquemacielcunha.model.csv.LinhaMescladaMunicipioBoletim;
import com.github.izaquemacielcunha.model.csv.LinhaMunicipioEnriquecida;

@Singleton
public class CsvMescladorBoletimServiceImpl implements CsvMescladorBoletimService {
    private static final Logger logger = LoggerFactory.getLogger(CsvMescladorBoletimServiceImpl.class);

    private final CsvMapper csvMapper;
    private final EleicaoConfig config;

    public CsvMescladorBoletimServiceImpl(CsvMapper csvMapper) {
        this(csvMapper, EleicaoConfig.padrao());
    }

    @Inject
    public CsvMescladorBoletimServiceImpl(CsvMapper csvMapper, EleicaoConfig config) {
        this.csvMapper = Objects.requireNonNull(csvMapper, "CsvMapper não pode ser nulo");
        this.config = Objects.requireNonNull(config, "EleicaoConfig não pode ser nulo");
    }

    @Override
    public void mesclarBoletinsComMunicipios() {
        try {
            mesclar(config.arquivoEnriquecidoCsv(), config.arquivoBoletimUrnaCsv(), config.arquivoResultadoMescladoCsv());
        } catch (IOException e) {
            logger.error("Falha ao mesclar votos com dados dos municípios", e);
            throw new EleicaoDataProcessingException("Erro ao mesclar votos com dados dos municípios", e);
        }
    }

    private int mesclar(Path fileUrnas, Path fileVotos, Path outputFile) throws IOException {
        Objects.requireNonNull(fileUrnas, "Arquivo de urnas não pode ser nulo");
        Objects.requireNonNull(fileVotos, "Arquivo de votos não pode ser nulo");
        Objects.requireNonNull(outputFile, "Arquivo de saída não pode ser nulo");

        return mesclar(fileUrnas.toFile(), fileVotos.toFile(), outputFile.toFile());
    }

    private int mesclar(File fileUrnas, File fileVotos, File outputFile) throws IOException {
        Objects.requireNonNull(fileUrnas, "Arquivo de urnas não pode ser nulo");
        Objects.requireNonNull(fileVotos, "Arquivo de votos não pode ser nulo");
        Objects.requireNonNull(outputFile, "Arquivo de saída não pode ser nulo");

        Instant inicio = Instant.now();

        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        CsvSchema schemaUrnas = csvMapper.schemaFor(LinhaMunicipioEnriquecida.class).withHeader();
        CsvSchema schemaVotos = csvMapper.schemaFor(LinhaBoletimUrna.class).withHeader();
        CsvSchema schemaMesclado = csvMapper.schemaFor(LinhaMescladaMunicipioBoletim.class).withHeader();

        Map<String, LinhaMunicipioEnriquecida> urnasMap = carregarMapaUrnas(fileUrnas, schemaUrnas);

        int totalMesclados = 0;
        try (MappingIterator<LinhaBoletimUrna> itVotos = csvMapper.readerFor(LinhaBoletimUrna.class)
                .with(schemaVotos)
                .readValues(fileVotos);
             SequenceWriter writer = csvMapper.writer(schemaMesclado).writeValues(outputFile)) {

            while (itVotos.hasNext()) {
                LinhaBoletimUrna voto = itVotos.next();
                LinhaMunicipioEnriquecida municipio = urnasMap.get(voto.boletimUrna());
                LinhaMescladaMunicipioBoletim linhaMesclada = criarLinhaMesclada(municipio, voto);
                writer.write(linhaMesclada);
                totalMesclados++;
            }
            writer.flush();
        }

        Duration decorrido = Duration.between(inicio, Instant.now());
        logger.info(
                "Mesclagem de votos concluída em {} ms. Total de registros mesclados: {}.",
                decorrido.toMillis(), totalMesclados);

        return totalMesclados;
    }

    private Map<String, LinhaMunicipioEnriquecida> carregarMapaUrnas(File fileUrnas, CsvSchema schemaUrnas) throws IOException {
        Map<String, LinhaMunicipioEnriquecida> urnasMap = new HashMap<>();
        try (MappingIterator<LinhaMunicipioEnriquecida> itUrnas = csvMapper.readerFor(LinhaMunicipioEnriquecida.class)
                .with(schemaUrnas)
                .readValues(fileUrnas)) {

            while (itUrnas.hasNext()) {
                LinhaMunicipioEnriquecida linha = itUrnas.next();
                urnasMap.put(linha.boletimUrna(), linha);
            }
        }
        return urnasMap;
    }

    private LinhaMescladaMunicipioBoletim criarLinhaMesclada(LinhaMunicipioEnriquecida municipio, LinhaBoletimUrna voto) {
        if (municipio != null) {
            return new LinhaMescladaMunicipioBoletim(
                    municipio.unidadeFederativa(),
                    municipio.codigoMunicipio(),
                    municipio.nomeMunicipio(),
                    municipio.numeroZona(),
                    municipio.numeroSecao(),
                    voto.candidato(),
                    voto.votos(),
                    municipio.hash(),
                    voto.boletimUrna()
            );
        }
        return new LinhaMescladaMunicipioBoletim(
                "", "", "", "", "",
                voto.candidato(),
                voto.votos(),
                "",
                voto.boletimUrna()
        );
    }
}
