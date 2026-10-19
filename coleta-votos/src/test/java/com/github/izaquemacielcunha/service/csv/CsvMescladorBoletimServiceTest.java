package com.github.izaquemacielcunha.service.csv;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.izaquemacielcunha.model.csv.LinhaBoletimUrna;
import com.github.izaquemacielcunha.model.csv.LinhaMescladaMunicipioBoletim;
import com.github.izaquemacielcunha.model.csv.LinhaMunicipioEnriquecida;

import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CsvMescladorBoletimServiceTest {

    private final CsvMapper csvMapper = new CsvMapper();
    private final CsvMescladorBoletimService service = new CsvMescladorBoletimService(csvMapper);

    @Test
    @DisplayName("Deve mesclar votos com informações enriquecidas de município com sucesso")
    void deveMesclarVotosComMunicipio(@TempDir Path tempDir) throws IOException {
        File fileUrnas = tempDir.resolve("enriquecido.csv").toFile();
        File fileVotos = tempDir.resolve("votos.csv").toFile();
        File fileSaida = tempDir.resolve("resultado.csv").toFile();

        // Grava arquivo de urnas enriquecido
        List<LinhaMunicipioEnriquecida> urnas = List.of(
                new LinhaMunicipioEnriquecida("PR", "75353", "CURITIBA", "1", "10", "hash-abc", "bu-10.dat")
        );
        CsvSchema schemaUrnas = csvMapper.schemaFor(LinhaMunicipioEnriquecida.class).withHeader();
        csvMapper.writer(schemaUrnas).writeValue(fileUrnas, urnas);

        // Grava arquivo de votos
        List<LinhaBoletimUrna> votos = List.of(
                new LinhaBoletimUrna("13", 150, "bu-10.dat"),
                new LinhaBoletimUrna("22", 120, "bu-10.dat")
        );
        CsvSchema schemaVotos = csvMapper.schemaFor(LinhaBoletimUrna.class).withHeader();
        csvMapper.writer(schemaVotos).writeValue(fileVotos, votos);

        // Executa a mescla
        service.mesclar(fileUrnas, fileVotos, fileSaida);

        // Lê e valida o resultado
        CsvSchema schemaMesclado = csvMapper.schemaFor(LinhaMescladaMunicipioBoletim.class).withHeader();
        List<LinhaMescladaMunicipioBoletim> resultado;
        try (MappingIterator<LinhaMescladaMunicipioBoletim> it = csvMapper.readerFor(LinhaMescladaMunicipioBoletim.class)
                .with(schemaMesclado)
                .readValues(fileSaida)) {
            resultado = it.readAll();
        }

        assertEquals(2, resultado.size());
        assertEquals("PR", resultado.getFirst().unidadeFederativa());
        assertEquals("CURITIBA", resultado.getFirst().nomeMunicipio());
        assertEquals("hash-abc", resultado.getFirst().hash());
        assertEquals("13", resultado.getFirst().candidato());
        assertEquals(150, resultado.getFirst().votos());
    }
}
