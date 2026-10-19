package com.github.izaquemacielcunha.service.csv;

import java.io.File;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

import com.github.izaquemacielcunha.model.Abrangencia;
import com.github.izaquemacielcunha.model.csv.LinhaMunicipio;

@Singleton
public class CsvService {

    private final CsvMapper mapper;

    @Inject
    public CsvService(CsvMapper mapper) {
        this.mapper = mapper;
    }

    public void gerarCsv(List<Abrangencia> abrangencias) {
        List<LinhaMunicipio> linhas = converterParaLinhas(abrangencias);

        CsvSchema schema = mapper.schemaFor(LinhaMunicipio.class).withHeader();

        mapper.writer(schema).writeValue(new File("temp-files/temp.csv"), linhas);
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
