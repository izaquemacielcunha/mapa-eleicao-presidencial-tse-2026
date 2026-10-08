package com.github.izaquemacielcunha.service.csv;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

import com.github.izaquemacielcunha.model.Abrangencia;
import com.github.izaquemacielcunha.model.csv.LinhaExportacaoCsv;

public class CsvService {

    public void gerarCsv(List<Abrangencia> abrangencias) {
        List<LinhaExportacaoCsv> linhas = converterParaLinhas(abrangencias);

        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper.schemaFor(LinhaExportacaoCsv.class)
                .withHeader();

        mapper.writer(schema).writeValue(new File("temp.csv"), linhas);
    }

    private List<LinhaExportacaoCsv> converterParaLinhas(List<Abrangencia> abrangencias) {
        return abrangencias.stream()
                .flatMap(abrangencia -> abrangencia.municipio().zonas().stream()
                        .flatMap(zona -> zona.secoes().stream()
                                .map(secao -> new LinhaExportacaoCsv(
                                        abrangencia.unidadeFederativa(),
                                        abrangencia.municipio().codigo(),
                                        abrangencia.municipio().nome(),
                                        zona.codigo(),
                                        secao.numero()
                                ))
                        )
                )
                .collect(Collectors.toList());
    }

}// end of class
