package com.github.izaquemacielcunha.service.urna.boletim;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

import com.github.izaquemacielcunha.config.EleicaoConfig;
import com.github.izaquemacielcunha.model.csv.LinhaMunicipioEnriquecida;
import com.github.izaquemacielcunha.service.ExternalService;
import com.github.izaquemacielcunha.service.tse.TseUrlBuilder;

@Singleton
public class BoletimUrnaDownloadService {
    private static final String INOPERANTE_AGREGADA = "inoperante/agregada";

    private final ExternalService externalService;
    private final CsvMapper mapper;
    private final TseUrlBuilder urlBuilder;
    private final EleicaoConfig config;

    @Inject
    public BoletimUrnaDownloadService(
            ExternalService externalService,
            CsvMapper mapper,
            TseUrlBuilder urlBuilder,
            EleicaoConfig config
    ) {
        this.externalService = externalService;
        this.mapper = mapper;
        this.urlBuilder = urlBuilder;
        this.config = config;
    }

    public void download() throws Exception {
        download(config.arquivoEnriquecidoCsv(), config.diretorioBoletins());
    }

    public void download(File arquivoEnriquecido, Path diretorioDestino) throws Exception {
        long start = System.nanoTime();

        CsvSchema schemaLeitura = mapper.schemaFor(LinhaMunicipioEnriquecida.class).withHeader();
        List<LinhaMunicipioEnriquecida> linhasOriginais;
        try (MappingIterator<LinhaMunicipioEnriquecida> iterador = mapper.readerFor(LinhaMunicipioEnriquecida.class)
                .with(schemaLeitura)
                .readValues(arquivoEnriquecido)) {
            linhasOriginais = iterador.readAll();
        }

        linhasOriginais.parallelStream().forEach(linha -> {
            try {
                if (!linha.boletimUrna().equalsIgnoreCase(INOPERANTE_AGREGADA)) {
                    externalService.baixarArquivoBoletimUrnaDat(
                            urlBuilder.buildUrlBoletimUrna(linha),
                            diretorioDestino
                    );
                }
            } catch (Exception e) {
                throw new RuntimeException("Erro ao baixar boletim da urna: " + linha.boletimUrna(), e);
            }
        });

        long finish = System.nanoTime();
        long timeElapsed = finish - start;

        System.out.println("Download concluído com sucesso! Tempo gasto: " + (timeElapsed / 1_000_000_000) + " s");
    }
}
