package com.github.izaquemacielcunha;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import com.github.izaquemacielcunha.model.Abrangencia;
import com.github.izaquemacielcunha.model.Municipio;
import com.github.izaquemacielcunha.model.csv.*;
import com.github.izaquemacielcunha.service.csv.CsvEnriquecedorService;
import com.github.izaquemacielcunha.service.csv.CsvService;
import com.github.izaquemacielcunha.service.urna.boletim.BoletimUrnaDownloadService;
import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.SequenceWriter;
import tools.jackson.dataformat.csv.CsvMapper;

import com.github.izaquemacielcunha.dependencyinjection.ApplicationComponent;
import com.github.izaquemacielcunha.dependencyinjection.DaggerApplicationComponent;
import com.github.izaquemacielcunha.model.urna.boletim.ResumoVotacao;
import com.github.izaquemacielcunha.service.ExternalServiceImpl;
import com.github.izaquemacielcunha.service.csv.CsvBoletimUrnaService;
import com.github.izaquemacielcunha.service.urna.boletim.BoletimUrnaService;
import tools.jackson.dataformat.csv.CsvSchema;

public class App {

    void main(String[] args) throws Exception {

        ApplicationComponent component = DaggerApplicationComponent.create();
        CsvMapper csvMapper = component.csvMapper();

        ExternalServiceImpl externalService = new ExternalServiceImpl(
                component.httpClient(),
                component.rateLimiter(),
                component.objectMapper()
        );

        //Fase 1 - Pegar infos municipios e salvar em csv ✅
        String uf = "PR";
        String codMun = "75353";
        String url = "https://resultados.tse.jus.br/oficial/ele2026/arquivo-urna/3220/config/"+uf.toLowerCase()+"/"+uf.toLowerCase()+"-p003220-cs.json";
        Optional<Municipio> resultado = externalService.callMunicipioConfig(url, codMun);
        Abrangencia abrangencia = new Abrangencia(uf, resultado.get());
        new CsvService(csvMapper).gerarCsv(List.of(abrangencia));

        //Fase 2 - Enriquecer csv salvo ✅
        CsvEnriquecedorService csvEnriquecedorService = new CsvEnriquecedorService(externalService, csvMapper);
        csvEnriquecedorService.enriquecer();

        //Fase 3 - Baixar boletins das urnas ✅
        BoletimUrnaDownloadService boletimUrnaDownloader = new BoletimUrnaDownloadService(externalService, csvMapper);
        boletimUrnaDownloader.download();

        //Fase 4 - Contabilizar votos e salvar ✅
        CsvBoletimUrnaService csvBoletimUrnaService = new CsvBoletimUrnaService(new CsvMapper());
        BoletimUrnaService boletimUrnaService = new BoletimUrnaService();
        List<String> listaBoletins = boletimUrnaService.listaArquivosBoletinsUrnas("temp-files/boletins-urnas");
        List<ResumoVotacao> votacao = listaBoletins.stream()
                .parallel()
                .flatMap(boletim -> {
                    try {
                        return boletimUrnaService.extrairResumoVotacao(
                                boletim,
                                boletim.substring(boletim.lastIndexOf('/') + 1)
                        ).stream();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
        csvBoletimUrnaService.process(votacao);

        //Fase 5  - Merge votos com csv enriquecido ✅
        CsvSchema schemaUrnas = csvMapper.schemaFor(LinhaMunicipioEnriquecida.class).withHeader();
        CsvSchema schemaVotos = csvMapper.schemaFor(LinhaBoletimUrna.class).withHeader();
        CsvSchema schemaMesclado = csvMapper.schemaFor(LinhaMescladaMunicipioBoletim.class).withHeader();

        Map<String, LinhaMunicipioEnriquecida> urnasMap = new HashMap<>();
        File fileUrnas = new File("temp-files/arquivo-enriquecido.csv");

        try (MappingIterator<LinhaMunicipioEnriquecida> itUrnas = csvMapper.readerFor(LinhaMunicipioEnriquecida.class)
                .with(schemaUrnas)
                .readValues(fileUrnas)) {

            while (itUrnas.hasNext()) {
                LinhaMunicipioEnriquecida linha = itUrnas.next();
                urnasMap.put(linha.boletimUrna(), linha);
            }
        }

        File fileVotos = new File("temp-files/arquivo-boletim-urna.csv");
        File outputFile = new File("temp-files/resultado-mesclado.csv");

        try (MappingIterator<LinhaBoletimUrna> itVotos = csvMapper.readerFor(LinhaBoletimUrna.class).with(schemaVotos).readValues(fileVotos);
             SequenceWriter writer = csvMapper.writer(schemaMesclado).writeValues(outputFile)) {

            while (itVotos.hasNext()) {
                LinhaBoletimUrna voto = itVotos.next();

                LinhaMunicipioEnriquecida municipio = urnasMap.get(voto.boletimUrna());

                LinhaMescladaMunicipioBoletim linhaMesclada;

                if (municipio != null) {
                    linhaMesclada = new LinhaMescladaMunicipioBoletim(
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
                } else {
                    linhaMesclada = new LinhaMescladaMunicipioBoletim(
                            "", "", "", "", "",
                            voto.candidato(),
                            voto.votos(),
                            "",
                            voto.boletimUrna()
                    );
                }

                writer.write(linhaMesclada);
            }
            writer.flush();
        }

        //Fase 6 - Gerar csv final ✅
        CsvSchema schemaCoordenadas = CsvSchema.emptySchema().withHeader().withColumnSeparator(';');
        CsvSchema schemaMesclaBase = csvMapper.schemaFor(LinhaMescladaMunicipioBoletim.class).withHeader().withColumnSeparator(',');
        CsvSchema schemaFinal = csvMapper.schemaFor(LinhaMunicipioBoletimCoordenadas.class).withHeader().withColumnSeparator(',');

        // 1. Carregar Coordenadas APENAS do município alvo na memória
        Map<String, LinhaMunicipioCoordenadas> coordenadasMap = new HashMap<>();
        try (BufferedReader readerCoords = Files.newBufferedReader(Paths.get("temp-files/eleitorado_local_votacao_2026_" + uf +".csv"), Charset.forName("ISO-8859-1"));
             MappingIterator<LinhaMunicipioCoordenadas> itCoords = csvMapper.readerFor(LinhaMunicipioCoordenadas.class)
                     .with(schemaCoordenadas)
                     .readValues(readerCoords)) {
            while (itCoords.hasNext()) {
                LinhaMunicipioCoordenadas coord = itCoords.next();

                //Código cidade aqui
                if (!coord.codigoMunicipio().equalsIgnoreCase(codMun)) {
                    continue;
                }

                coordenadasMap.put(coord.gerarChaveComposta(), coord);
            }
        }

        // 2. Iterar sobre a sua planilha já mesclada e adicionar as coordenadas
        try (MappingIterator<LinhaMescladaMunicipioBoletim> itMesclaBase = csvMapper.readerFor(LinhaMescladaMunicipioBoletim.class)
                .with(schemaMesclaBase)
                .readValues(new File("temp-files/resultado-mesclado.csv")); // Seu arquivo atual
             SequenceWriter writer = csvMapper.writer(schemaFinal)
                     .writeValues(new File("carto-dataset-final-eleic-presi-2026-1-turn-" + uf.toLowerCase() + ".csv"))) {

            while (itMesclaBase.hasNext()) {
                LinhaMescladaMunicipioBoletim linhaAtual = itMesclaBase.next();

                // Recria a chave composta (UF-MUNICIPIO-ZONA-SECAO) para buscar no mapa
                String chaveBusca = linhaAtual.unidadeFederativa() + "-" +
                        linhaAtual.codigoMunicipio() + "-" +
                        Integer.valueOf(linhaAtual.numeroZona()) + "-" +
                        Integer.valueOf(linhaAtual.numeroSecao());

                LinhaMunicipioCoordenadas coordenada = coordenadasMap.get(chaveBusca);

                // Previne nulos usando empty strings se a seção não for encontrada nas coordenadas
                String bairro = coordenada != null ? coordenada.nomeBairro() : "";
                String local = coordenada != null ? coordenada.localVotacao() : "";
                String lat = coordenada != null ? coordenada.latitude() : "";
                String lon = coordenada != null ? coordenada.longitude() : "";

                // Cria o record final consolidado para o Carto
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
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Arquivo gerado e pronto para o Carto.com!");

    }

    //eleicao-presidencial-2026-1-turno
}// end of class