package com.github.izaquemacielcunha.service.csv;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.izaquemacielcunha.config.EleicaoConfig;
import com.github.izaquemacielcunha.model.csv.LinhaBoletimUrna;
import com.github.izaquemacielcunha.model.csv.LinhaMescladaMunicipioBoletim;
import com.github.izaquemacielcunha.model.csv.LinhaMunicipioEnriquecida;

import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvMescladorBoletimServiceTest {

    private final CsvMapper csvMapper = new CsvMapper();

    @Test
    @DisplayName("Deve mesclar votos com informações enriquecidas de município com sucesso")
    void deveMesclarVotosComMunicipio(@TempDir Path tempDir) throws IOException {
        EleicaoConfig config = new EleicaoConfig(
                "PR",
                "75353",
                "3220",
                "ele2026",
                tempDir,
                tempDir.resolve("boletins")
        );
        CsvMescladorBoletimService service = new CsvMescladorBoletimServiceImpl(csvMapper, config);

        // Grava arquivo de urnas enriquecido
        List<LinhaMunicipioEnriquecida> urnas = List.of(
                new LinhaMunicipioEnriquecida("PR", "75353", "CURITIBA", "1", "10", "hash-abc", "bu-10.dat")
        );
        CsvSchema schemaUrnas = csvMapper.schemaFor(LinhaMunicipioEnriquecida.class).withHeader();
        csvMapper.writer(schemaUrnas).writeValue(config.arquivoEnriquecidoCsv().toFile(), urnas);

        // Grava arquivo de votos
        List<LinhaBoletimUrna> votos = List.of(
                new LinhaBoletimUrna("13", 150, "bu-10.dat"),
                new LinhaBoletimUrna("22", 120, "bu-10.dat")
        );
        CsvSchema schemaVotos = csvMapper.schemaFor(LinhaBoletimUrna.class).withHeader();
        csvMapper.writer(schemaVotos).writeValue(config.arquivoBoletimUrnaCsv().toFile(), votos);

        // Executa a mescla via método de serviço
        service.mesclarBoletinsComMunicipios();

        // Lê e valida o resultado
        CsvSchema schemaMesclado = csvMapper.schemaFor(LinhaMescladaMunicipioBoletim.class).withHeader();
        List<LinhaMescladaMunicipioBoletim> resultado;
        try (MappingIterator<LinhaMescladaMunicipioBoletim> it = csvMapper.readerFor(LinhaMescladaMunicipioBoletim.class)
                .with(schemaMesclado)
                .readValues(config.arquivoResultadoMescladoCsv().toFile())) {
            resultado = it.readAll();
        }

        assertEquals(2, resultado.size());
        assertEquals("PR", resultado.getFirst().unidadeFederativa());
        assertEquals("CURITIBA", resultado.getFirst().nomeMunicipio());
        assertEquals("hash-abc", resultado.getFirst().hash());
        assertEquals("13", resultado.getFirst().candidato());
        assertEquals(150, resultado.getFirst().votos());
    }

    @Test
    @DisplayName("mesclarBoletinsComMunicipios deve utilizar os caminhos injetados por EleicaoConfig")
    void deveMesclarUsandoConfiguracaoInjetada(@TempDir Path tempDir) throws IOException {
        EleicaoConfig config = new EleicaoConfig(
                "PR",
                "75353",
                "3220",
                "ele2026",
                tempDir,
                tempDir.resolve("boletins")
        );
        CsvMescladorBoletimService service = new CsvMescladorBoletimServiceImpl(csvMapper, config);

        List<LinhaMunicipioEnriquecida> urnas = List.of(
                new LinhaMunicipioEnriquecida("PR", "75353", "CURITIBA", "1", "10", "hash-xyz", "bu-20.dat")
        );
        CsvSchema schemaUrnas = csvMapper.schemaFor(LinhaMunicipioEnriquecida.class).withHeader();
        csvMapper.writer(schemaUrnas).writeValue(config.arquivoEnriquecidoCsv().toFile(), urnas);

        List<LinhaBoletimUrna> votos = List.of(
                new LinhaBoletimUrna("13", 200, "bu-20.dat")
        );
        CsvSchema schemaVotos = csvMapper.schemaFor(LinhaBoletimUrna.class).withHeader();
        csvMapper.writer(schemaVotos).writeValue(config.arquivoBoletimUrnaCsv().toFile(), votos);

        service.mesclarBoletinsComMunicipios();

        CsvSchema schemaMesclado = csvMapper.schemaFor(LinhaMescladaMunicipioBoletim.class).withHeader();
        List<LinhaMescladaMunicipioBoletim> resultado;
        try (MappingIterator<LinhaMescladaMunicipioBoletim> it = csvMapper.readerFor(LinhaMescladaMunicipioBoletim.class)
                .with(schemaMesclado)
                .readValues(config.arquivoResultadoMescladoCsv().toFile())) {
            resultado = it.readAll();
        }

        assertEquals(1, resultado.size());
        assertEquals("hash-xyz", resultado.getFirst().hash());
        assertEquals(200, resultado.getFirst().votos());
    }
}
