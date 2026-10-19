package com.github.izaquemacielcunha.service.urna.boletim;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

import com.github.izaquemacielcunha.model.csv.LinhaMunicipioEnriquecida;
import com.github.izaquemacielcunha.service.ExternalServiceImpl;


@Singleton
public class BoletimUrnaDownloadService {
    private static final String INOPERANTE_AGREGADA = "inoperante/agregada";

    private final ExternalServiceImpl externalService;
    private final CsvMapper mapper;

    @Inject
    public BoletimUrnaDownloadService(ExternalServiceImpl externalService, CsvMapper mapper) {
        this.externalService = externalService;
        this.mapper = mapper;
    }

    public void download() throws Exception {

        long start = System.nanoTime();

        CsvSchema schemaLeitura = mapper.schemaFor(LinhaMunicipioEnriquecida.class).withHeader();
        MappingIterator<LinhaMunicipioEnriquecida> iterador = mapper.readerFor(LinhaMunicipioEnriquecida.class)
                .with(schemaLeitura)
                .readValues(new File("temp-files/arquivo-enriquecido.csv"));

        List<LinhaMunicipioEnriquecida> linhasOriginais = iterador.readAll();

        linhasOriginais.parallelStream().forEach(linhaExportacaoEnriquecidaCsv -> {
            try {
                if (!linhaExportacaoEnriquecidaCsv.boletimUrna().equalsIgnoreCase(INOPERANTE_AGREGADA)) {
                    externalService.baixarArquivoBoletimUrnaDat(montarUrl(linhaExportacaoEnriquecidaCsv), Path.of("temp-files/boletins-urnas"));
                }
            }
            catch (Exception e) {
                throw  new RuntimeException(e);
            }
        });

        long finish = System.nanoTime();
        long timeElapsed = finish - start;

        System.out.println("Download concluído com sucesso! Tempo gasto: " + (timeElapsed / 1_000_000_000) + " s");
    }

    private String montarUrl(LinhaMunicipioEnriquecida linha) {
        String baseUrl = "https://resultados.tse.jus.br/oficial/ele2026/arquivo-urna/3220/dados/%s/%s/%s/%s/%s/%s";

        return String.format(baseUrl, linha.unidadeFederativa().toLowerCase(), linha.codigoMunicipio(),
                linha.numeroZona(), linha.numeroSecao(), linha.hash(), linha.boletimUrna());
    }


}// end of class
