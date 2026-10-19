package com.github.izaquemacielcunha.service.csv;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.SequenceWriter;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

import com.github.izaquemacielcunha.model.csv.LinhaBoletimUrna;
import com.github.izaquemacielcunha.model.csv.LinhaMescladaMunicipioBoletim;
import com.github.izaquemacielcunha.model.csv.LinhaMunicipioEnriquecida;

@Singleton
public class CsvMescladorBoletimService {

    private static final File DEFAULT_ARQUIVO_ENRIQUECIDO = new File("temp-files/arquivo-enriquecido.csv");
    private static final File DEFAULT_ARQUIVO_VOTOS = new File("temp-files/arquivo-boletim-urna.csv");
    private static final File DEFAULT_ARQUIVO_SAIDA = new File("temp-files/resultado-mesclado.csv");

    private final CsvMapper csvMapper;

    @Inject
    public CsvMescladorBoletimService(CsvMapper csvMapper) {
        this.csvMapper = csvMapper;
    }

    public void mesclar() throws IOException {
        mesclar(DEFAULT_ARQUIVO_ENRIQUECIDO, DEFAULT_ARQUIVO_VOTOS, DEFAULT_ARQUIVO_SAIDA);
    }

    public void mesclar(File fileUrnas, File fileVotos, File outputFile) throws IOException {
        CsvSchema schemaUrnas = csvMapper.schemaFor(LinhaMunicipioEnriquecida.class).withHeader();
        CsvSchema schemaVotos = csvMapper.schemaFor(LinhaBoletimUrna.class).withHeader();
        CsvSchema schemaMesclado = csvMapper.schemaFor(LinhaMescladaMunicipioBoletim.class).withHeader();

        Map<String, LinhaMunicipioEnriquecida> urnasMap = carregarMapaUrnas(fileUrnas, schemaUrnas);

        try (MappingIterator<LinhaBoletimUrna> itVotos = csvMapper.readerFor(LinhaBoletimUrna.class)
                .with(schemaVotos)
                .readValues(fileVotos);
             SequenceWriter writer = csvMapper.writer(schemaMesclado).writeValues(outputFile)) {

            while (itVotos.hasNext()) {
                LinhaBoletimUrna voto = itVotos.next();
                LinhaMunicipioEnriquecida municipio = urnasMap.get(voto.boletimUrna());
                LinhaMescladaMunicipioBoletim linhaMesclada = criarLinhaMesclada(municipio, voto);
                writer.write(linhaMesclada);
            }
            writer.flush();
        }
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
