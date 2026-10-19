package com.github.izaquemacielcunha.service.csv;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.SequenceWriter;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

import com.github.izaquemacielcunha.model.csv.LinhaMescladaMunicipioBoletim;
import com.github.izaquemacielcunha.model.csv.LinhaMunicipioBoletimCoordenadas;
import com.github.izaquemacielcunha.model.csv.LinhaMunicipioCoordenadas;

@Singleton
public class CsvCartoExportadorService {

    private final CsvMapper csvMapper;

    @Inject
    public CsvCartoExportadorService(CsvMapper csvMapper) {
        this.csvMapper = csvMapper;
    }

    public void exportar(Path arquivoCoordenadas, File arquivoMesclado, File arquivoDestino, String codigoMunicipioAlvo) throws IOException {
        CsvSchema schemaCoordenadas = CsvSchema.emptySchema().withHeader().withColumnSeparator(';');
        CsvSchema schemaMesclaBase = csvMapper.schemaFor(LinhaMescladaMunicipioBoletim.class).withHeader().withColumnSeparator(',');
        CsvSchema schemaFinal = csvMapper.schemaFor(LinhaMunicipioBoletimCoordenadas.class).withHeader().withColumnSeparator(',');

        Map<String, LinhaMunicipioCoordenadas> coordenadasMap = carregarCoordenadas(arquivoCoordenadas, schemaCoordenadas, codigoMunicipioAlvo);

        try (MappingIterator<LinhaMescladaMunicipioBoletim> itMesclaBase = csvMapper.readerFor(LinhaMescladaMunicipioBoletim.class)
                .with(schemaMesclaBase)
                .readValues(arquivoMesclado);
             SequenceWriter writer = csvMapper.writer(schemaFinal).writeValues(arquivoDestino)) {

            while (itMesclaBase.hasNext()) {
                LinhaMescladaMunicipioBoletim linhaAtual = itMesclaBase.next();
                LinhaMunicipioCoordenadas coordenada = coordenadasMap.get(linhaAtual.gerarChaveComposta());

                String bairro = coordenada != null ? coordenada.nomeBairro() : "";
                String local = coordenada != null ? coordenada.localVotacao() : "";
                String lat = coordenada != null ? coordenada.latitude() : "";
                String lon = coordenada != null ? coordenada.longitude() : "";

                LinhaMunicipioBoletimCoordenadas linhaFinal = new LinhaMunicipioBoletimCoordenadas(
                        linhaAtual.unidadeFederativa(),
                        linhaAtual.codigoMunicipio(),
                        linhaAtual.nomeMunicipio(),
                        linhaAtual.numeroZona(),
                        linhaAtual.numeroSecao(),
                        bairro,
                        local,
                        lat.replace(",", "."),
                        lon.replace(",", "."),
                        linhaAtual.candidato(),
                        linhaAtual.votos(),
                        linhaAtual.boletimUrna()
                );

                writer.write(linhaFinal);
            }
            writer.flush();
        }
    }

    private Map<String, LinhaMunicipioCoordenadas> carregarCoordenadas(
            Path arquivoCoordenadas,
            CsvSchema schemaCoordenadas,
            String codigoMunicipioAlvo
    ) throws IOException {
        Map<String, LinhaMunicipioCoordenadas> coordenadasMap = new HashMap<>();

        try (BufferedReader readerCoords = Files.newBufferedReader(arquivoCoordenadas, Charset.forName("ISO-8859-1"));
             MappingIterator<LinhaMunicipioCoordenadas> itCoords = csvMapper.readerFor(LinhaMunicipioCoordenadas.class)
                     .with(schemaCoordenadas)
                     .readValues(readerCoords)) {

            while (itCoords.hasNext()) {
                LinhaMunicipioCoordenadas coord = itCoords.next();
                if (coord.codigoMunicipio() != null && coord.codigoMunicipio().equalsIgnoreCase(codigoMunicipioAlvo)) {
                    coordenadasMap.put(coord.gerarChaveComposta(), coord);
                }
            }
        }
        return coordenadasMap;
    }
}
