package com.github.izaquemacielcunha.service.csv;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.SequenceWriter;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

import com.github.izaquemacielcunha.exception.EleicaoDataProcessingException;
import com.github.izaquemacielcunha.model.csv.LinhaMescladaMunicipioBoletim;
import com.github.izaquemacielcunha.model.csv.LinhaMunicipioBoletimCoordenadas;
import com.github.izaquemacielcunha.model.csv.LinhaMunicipioCoordenadas;

@Singleton
public class CsvCartoExportadorServiceImpl implements CsvCartoExportadorService {
    private static final Logger logger = LoggerFactory.getLogger(CsvCartoExportadorServiceImpl.class);

    private record ResultadoExportacaoCarto(int totalExportados, int totalSemCoordenadas) {}

    private final CsvMapper csvMapper;

    @Inject
    public CsvCartoExportadorServiceImpl(CsvMapper csvMapper) {
        this.csvMapper = Objects.requireNonNull(csvMapper, "CsvMapper não pode ser nulo");
    }

    @Override
    public void exportarDatasetCarto(Path arquivoCoordenadasTse, Path arquivoMesclado, Path arquivoDestino, String codigoMunicipioAlvo) {
        try {
            exportar(arquivoCoordenadasTse, arquivoMesclado, arquivoDestino, codigoMunicipioAlvo);
        } catch (IOException e) {
            logger.error("Erro ao exportar dataset final para o Carto", e);
            throw new EleicaoDataProcessingException("Erro ao exportar dataset final para o Carto", e);
        }
    }

    private ResultadoExportacaoCarto exportar(Path arquivoCoordenadas, Path arquivoMesclado, Path arquivoDestino, String codigoMunicipioAlvo) throws IOException {
        Objects.requireNonNull(arquivoMesclado, "Arquivo mesclado não pode ser nulo");
        Objects.requireNonNull(arquivoDestino, "Arquivo destino não pode ser nulo");
        return exportar(arquivoCoordenadas, arquivoMesclado.toFile(), arquivoDestino.toFile(), codigoMunicipioAlvo);
    }

    private ResultadoExportacaoCarto exportar(Path arquivoCoordenadas, File arquivoMesclado, File arquivoDestino, String codigoMunicipioAlvo) throws IOException {
        Objects.requireNonNull(arquivoCoordenadas, "Arquivo de coordenadas não pode ser nulo");
        Objects.requireNonNull(arquivoMesclado, "Arquivo mesclado não pode ser nulo");
        Objects.requireNonNull(arquivoDestino, "Arquivo destino não pode ser nulo");
        Objects.requireNonNull(codigoMunicipioAlvo, "Código do município alvo não pode ser nulo");

        Instant inicio = Instant.now();

        CsvSchema schemaCoordenadas = CsvSchema.emptySchema().withHeader().withColumnSeparator(';');
        CsvSchema schemaMesclaBase = csvMapper.schemaFor(LinhaMescladaMunicipioBoletim.class).withHeader().withColumnSeparator(',');
        CsvSchema schemaFinal = csvMapper.schemaFor(LinhaMunicipioBoletimCoordenadas.class).withHeader().withColumnSeparator(',');

        Map<String, LinhaMunicipioCoordenadas> coordenadasMap = carregarCoordenadas(arquivoCoordenadas, schemaCoordenadas, codigoMunicipioAlvo);

        int totalExportados = 0;
        int totalSemCoordenadas = 0;

        try (MappingIterator<LinhaMescladaMunicipioBoletim> itMesclaBase = csvMapper.readerFor(LinhaMescladaMunicipioBoletim.class)
                .with(schemaMesclaBase)
                .readValues(arquivoMesclado);
             SequenceWriter writer = csvMapper.writer(schemaFinal).writeValues(arquivoDestino)) {

            while (itMesclaBase.hasNext()) {
                LinhaMescladaMunicipioBoletim linhaAtual = itMesclaBase.next();
                LinhaMunicipioCoordenadas coordenada = coordenadasMap.get(linhaAtual.gerarChaveComposta());

                if (coordenada == null) {
                    totalSemCoordenadas++;
                    logger.warn(
                            "Seção sem coordenadas correspondentes no arquivo do Carto/TSE: chave={}, boletim={}",
                            linhaAtual.gerarChaveComposta(), linhaAtual.boletimUrna());
                }

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
                totalExportados++;
            }
            writer.flush();
        }

        Duration decorrido = Duration.between(inicio, Instant.now());
        logger.info(
                "Exportação para Carto concluída em {} ms. Total de registros exportados: {} (sem coordenadas: {}).",
                decorrido.toMillis(), totalExportados, totalSemCoordenadas);

        return new ResultadoExportacaoCarto(totalExportados, totalSemCoordenadas);
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
