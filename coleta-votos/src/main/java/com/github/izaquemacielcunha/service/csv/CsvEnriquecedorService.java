package com.github.izaquemacielcunha.service.csv;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

import com.github.izaquemacielcunha.model.csv.LinhaEnriquecidaCsv;
import com.github.izaquemacielcunha.model.csv.LinhaExportacaoCsv;
import com.github.izaquemacielcunha.model.urnas.Metadados;
import com.github.izaquemacielcunha.service.ExternalServiceImpl;

@Singleton
public class CsvEnriquecedorService {
    private static final String BOLETIM_URNA = "bu";

    private final ExternalServiceImpl externalService;
    private final CsvMapper mapper;

    @Inject
    public CsvEnriquecedorService(ExternalServiceImpl externalService, CsvMapper mapper) {
        this.externalService = externalService;
        this.mapper = mapper;
    }

    public void enriquecer() throws Exception {

        long start = System.nanoTime();

        CsvSchema schemaLeitura = mapper.schemaFor(LinhaExportacaoCsv.class).withHeader();
        MappingIterator<LinhaExportacaoCsv> iterador = mapper.readerFor(LinhaExportacaoCsv.class)
                .with(schemaLeitura)
                .readValues(new File("temp.csv"));

        List<LinhaExportacaoCsv> linhasOriginais = iterador.readAll();

        List<LinhaEnriquecidaCsv> linhasEnriquecidas = linhasOriginais.parallelStream().map(linha -> {
            try {
                Optional<Metadados> dados = externalService.callZonaSecaoInfos(montarUrl(linha));
                if (Objects.nonNull(dados.get().hashes())) {
                    return new LinhaEnriquecidaCsv(
                            linha.unidadeFederativa(),
                            linha.codigoMunicipio(),
                            linha.nomeMunicipio(),
                            linha.numeroZona(),
                            linha.numeroSecao(),
                            dados.get().hashes().getFirst().hash(),
                            dados.get().hashes().getFirst().arquivos()
                                    .stream()
                                    .filter(arquivo -> arquivo.tipo().equals(BOLETIM_URNA))
                                    .findFirst()
                                    .get().nome()
                    );
                }
                return new LinhaEnriquecidaCsv(
                        linha.unidadeFederativa(),
                        linha.codigoMunicipio(),
                        linha.nomeMunicipio(),
                        linha.numeroZona(),
                        linha.numeroSecao(),
                        "inoperante/agregada",
                        "inoperante/agregada");
            }
            catch (Exception e) {
                throw new RuntimeException("[enriquecer] - Erro ao enriquecer csv. Linha: " + linha, e);
            }
        }).collect(Collectors.toList());

        CsvSchema schemaEscrita = mapper.schemaFor(LinhaEnriquecidaCsv.class).withHeader();
        mapper.writer(schemaEscrita).writeValue(new File("arquivo-enriquecido.csv"), linhasEnriquecidas);

        long finish = System.nanoTime();
        long timeElapsed = finish - start;

        System.out.println("Processamento concluído com sucesso! Tempo gasto: " + (timeElapsed / 1_000_000_000) + " s");
    }

    private String montarUrl(LinhaExportacaoCsv linha) {
        String baseUrl = "https://resultados.tse.jus.br/oficial/ele2026/arquivo-urna/3220/dados/%s/%s/%s/%s/p003220-%s-m%s-z%s-s%s-aux.json";
        return String.format(baseUrl, linha.unidadeFederativa().toLowerCase(), linha.codigoMunicipio(), linha.numeroZona(),
                linha.numeroSecao(), linha.unidadeFederativa().toLowerCase(), linha.codigoMunicipio(), linha.numeroZona(),
                linha.numeroSecao());
    }

}// end of class
