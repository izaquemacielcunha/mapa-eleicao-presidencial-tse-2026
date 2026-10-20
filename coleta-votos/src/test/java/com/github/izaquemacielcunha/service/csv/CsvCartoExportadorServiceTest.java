package com.github.izaquemacielcunha.service.csv;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.izaquemacielcunha.model.csv.LinhaMescladaMunicipioBoletim;
import com.github.izaquemacielcunha.model.csv.LinhaMunicipioBoletimCoordenadas;

import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvCartoExportadorServiceTest {

    private final CsvMapper csvMapper = new CsvMapper();
    private final CsvCartoExportadorService service = new CsvCartoExportadorServiceImpl(csvMapper);

    @Test
    @DisplayName("Deve exportar dataset do Carto cruzando coordenadas e normalizando latitude/longitude")
    void deveExportarDatasetCartoComCoordenadas(@TempDir Path tempDir) throws IOException {
        Path arquivoCoords = tempDir.resolve("coordenadas.csv");
        Path arquivoMesclado = tempDir.resolve("mesclado.csv");
        Path arquivoCarto = tempDir.resolve("carto-saida.csv");

        // Escreve CSV de coordenadas no padrão do TSE (; e ISO-8859-1)
        try (BufferedWriter writer = Files.newBufferedWriter(arquivoCoords, Charset.forName("ISO-8859-1"))) {
            writer.write("SG_UF;CD_MUNICIPIO;NM_MUNICIPIO;NR_ZONA;NR_SECAO;NM_BAIRRO;NR_LATITUDE;NR_LONGITUDE;NM_LOCAL_VOTACAO\n");
            writer.write("PR;75353;CURITIBA;1;10;CENTRO;-25,4284;-49,2733;COLEGIO ESTADUAL\n");
            writer.write("PR;99999;OUTRA_CIDADE;1;10;BAIRRO;-20,0000;-40,0000;OUTRO LOCAL\n");
        }

        // Escreve arquivo mesclado (votos + urnas) com zona e seção contendo zeros à esquerda
        List<LinhaMescladaMunicipioBoletim> mesclado = List.of(
                new LinhaMescladaMunicipioBoletim("PR", "75353", "CURITIBA", "001", "00010", "13", 100, "hash", "bu-10.dat")
        );
        CsvSchema schemaMesclaBase = csvMapper.schemaFor(LinhaMescladaMunicipioBoletim.class).withHeader().withColumnSeparator(',');
        csvMapper.writer(schemaMesclaBase).writeValue(arquivoMesclado.toFile(), mesclado);

        // Exporta para Carto filtrando pelo município 75353
        service.exportarDatasetCarto(arquivoCoords, arquivoMesclado, arquivoCarto, "75353");

        // Valida cabeçalho com labels em maiúsculas
        List<String> linhasArquivo = Files.readAllLines(arquivoCarto);
        assertEquals("SG_UF,CD_MUNICIPIO,NM_MUNICIPIO,NR_ZONA,NR_SECAO,NM_BAIRRO,NM_LOCAL_VOTACAO,LATITUDE,LONGITUDE,CANDIDATO,QTD_VOTOS,BOLETIM_URNA", linhasArquivo.getFirst());

        // Lê e valida o arquivo gerado
        CsvSchema schemaFinal = csvMapper.schemaFor(LinhaMunicipioBoletimCoordenadas.class).withHeader().withColumnSeparator(',');
        List<LinhaMunicipioBoletimCoordenadas> resultado;
        try (MappingIterator<LinhaMunicipioBoletimCoordenadas> it = csvMapper.readerFor(LinhaMunicipioBoletimCoordenadas.class)
                .with(schemaFinal)
                .readValues(arquivoCarto.toFile())) {
            resultado = it.readAll();
        }

        assertEquals(1, resultado.size());
        LinhaMunicipioBoletimCoordenadas linha = resultado.getFirst();
        assertEquals("PR", linha.unidadeFederativa());
        assertEquals("75353", linha.codigoMunicipio());
        assertEquals("CENTRO", linha.nomeBairro());
        assertEquals("COLEGIO ESTADUAL", linha.localVotacao());
        assertEquals("-25.4284", linha.latitude());
        assertEquals("-49.2733", linha.longitude());
        assertEquals("13", linha.candidato());
        assertEquals(100, linha.votos());
    }

    @Test
    @DisplayName("exportarDatasetCarto deve executar o fluxo e gerar o arquivo de destino")
    void deveExecutarExportarDatasetCarto(@TempDir Path tempDir) throws IOException {
        Path arquivoCoords = tempDir.resolve("coords.csv");
        Path arquivoMesclado = tempDir.resolve("mesclado.csv");
        Path arquivoDestino = tempDir.resolve("carto-dest.csv");

        Files.writeString(arquivoCoords, "SG_UF;CD_MUNICIPIO;NM_MUNICIPIO;NR_ZONA;NR_SECAO;NM_BAIRRO;NR_LATITUDE;NR_LONGITUDE;NM_LOCAL_VOTACAO\n");
        Files.writeString(arquivoMesclado, "unidadeFederativa,codigoMunicipio,nomeMunicipio,numeroZona,numeroSecao,candidato,votos,hash,boletimUrna\n");

        service.exportarDatasetCarto(arquivoCoords, arquivoMesclado, arquivoDestino, "75353");

        assertEquals(true, Files.exists(arquivoDestino));
    }
}
