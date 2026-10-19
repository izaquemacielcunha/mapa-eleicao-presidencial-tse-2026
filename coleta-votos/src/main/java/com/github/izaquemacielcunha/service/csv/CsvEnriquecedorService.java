package com.github.izaquemacielcunha.service.csv;

import java.io.File;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

import com.github.izaquemacielcunha.model.csv.LinhaMunicipioEnriquecida;
import com.github.izaquemacielcunha.model.csv.LinhaMunicipio;
import com.github.izaquemacielcunha.model.urna.Arquivo;
import com.github.izaquemacielcunha.model.urna.Metadados;
import com.github.izaquemacielcunha.service.ExternalService;
import com.github.izaquemacielcunha.service.tse.TseUrlBuilder;

@Singleton
public class CsvEnriquecedorService {
    private static final String BOLETIM_URNA = "bu";
    private static final String BOLETIM_URNA_BUSA = "busa";
    private static final String INOPERANTE_AGREGADA = "inoperante/agregada";

    private final ExternalService externalService;
    private final CsvMapper mapper;
    private final TseUrlBuilder urlBuilder;

    @Inject
    public CsvEnriquecedorService(ExternalService externalService, CsvMapper mapper, TseUrlBuilder urlBuilder) {
        this.externalService = externalService;
        this.mapper = mapper;
        this.urlBuilder = urlBuilder;
    }

    public void enriquecer() throws Exception {

        long start = System.nanoTime();

        CsvSchema schemaLeitura = mapper.schemaFor(LinhaMunicipio.class).withHeader();
        List<LinhaMunicipio> linhasOriginais;
        try (MappingIterator<LinhaMunicipio> iterador = mapper.readerFor(LinhaMunicipio.class)
                .with(schemaLeitura)
                .readValues(new File("temp-files/temp.csv"))) {
            linhasOriginais = iterador.readAll();
        }

        List<LinhaMunicipioEnriquecida> linhasEnriquecidas = linhasOriginais.parallelStream().map(linha -> {
            try {
                Optional<Metadados> dados = externalService.callZonaSecaoInfos(urlBuilder.buildUrlSecaoMetadados(linha));
                if (dados.isPresent() && dados.get().hashes() != null && !dados.get().hashes().isEmpty()) {
                    var hashInfo = dados.get().hashes().getFirst();
                    String buNome = hashInfo.arquivos() != null ? hashInfo.arquivos()
                            .stream()
                            .filter(arquivo -> BOLETIM_URNA.equals(arquivo.tipo()) || BOLETIM_URNA_BUSA.equals(arquivo.tipo()))
                            .map(Arquivo::nome)
                            .findFirst()
                            .orElse(INOPERANTE_AGREGADA) : INOPERANTE_AGREGADA;

                    return new LinhaMunicipioEnriquecida(
                            linha.unidadeFederativa(),
                            linha.codigoMunicipio(),
                            linha.nomeMunicipio(),
                            linha.numeroZona(),
                            linha.numeroSecao(),
                            hashInfo.hash(),
                            buNome
                    );
                }
                return new LinhaMunicipioEnriquecida(
                        linha.unidadeFederativa(),
                        linha.codigoMunicipio(),
                        linha.nomeMunicipio(),
                        linha.numeroZona(),
                        linha.numeroSecao(),
                        INOPERANTE_AGREGADA,
                        INOPERANTE_AGREGADA);
            }
            catch (Exception e) {
                throw new RuntimeException("[enriquecer] - Erro ao enriquecer csv. Linha: " + linha, e);
            }
        }).toList();

        CsvSchema schemaEscrita = mapper.schemaFor(LinhaMunicipioEnriquecida.class).withHeader();
        mapper.writer(schemaEscrita).writeValue(new File("temp-files/arquivo-enriquecido.csv"), linhasEnriquecidas);

        long finish = System.nanoTime();
        long timeElapsed = finish - start;

        System.out.println("Processamento concluído com sucesso! Tempo gasto: " + (timeElapsed / 1_000_000_000) + " s");
    }

}
