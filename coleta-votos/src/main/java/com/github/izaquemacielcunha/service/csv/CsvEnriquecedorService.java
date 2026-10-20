package com.github.izaquemacielcunha.service.csv;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

import com.github.izaquemacielcunha.config.EleicaoConfig;
import com.github.izaquemacielcunha.exception.EleicaoDataProcessingException;
import com.github.izaquemacielcunha.model.csv.LinhaMunicipioEnriquecida;
import com.github.izaquemacielcunha.model.csv.LinhaMunicipio;
import com.github.izaquemacielcunha.model.urna.Arquivo;
import com.github.izaquemacielcunha.model.urna.Metadados;
import com.github.izaquemacielcunha.model.urna.StatusSecaoEleitoral;
import com.github.izaquemacielcunha.model.urna.TipoArquivoUrna;
import com.github.izaquemacielcunha.service.ExternalService;
import com.github.izaquemacielcunha.service.tse.TseUrlBuilder;

@Singleton
public class CsvEnriquecedorService {
    private static final Logger logger = LoggerFactory.getLogger(CsvEnriquecedorService.class);

    public record ResultadoEnriquecimento(int totalSecoes, int totalAtivas, int totalInoperantes) {}

    private final ExternalService externalService;
    private final CsvMapper mapper;
    private final TseUrlBuilder urlBuilder;
    private final EleicaoConfig config;

    public CsvEnriquecedorService(ExternalService externalService, CsvMapper mapper, TseUrlBuilder urlBuilder) {
        this(externalService, mapper, urlBuilder, EleicaoConfig.padrao());
    }

    @Inject
    public CsvEnriquecedorService(ExternalService externalService, CsvMapper mapper, TseUrlBuilder urlBuilder, EleicaoConfig config) {
        this.externalService = Objects.requireNonNull(externalService, "ExternalService não pode ser nulo");
        this.mapper = Objects.requireNonNull(mapper, "CsvMapper não pode ser nulo");
        this.urlBuilder = Objects.requireNonNull(urlBuilder, "TseUrlBuilder não pode ser nulo");
        this.config = Objects.requireNonNull(config, "EleicaoConfig não pode ser nulo");
    }

    public ResultadoEnriquecimento enriquecer() {
        Instant inicio = Instant.now();

        CsvSchema schemaLeitura = mapper.schemaFor(LinhaMunicipio.class).withHeader();
        List<LinhaMunicipio> linhasOriginais;
        try (MappingIterator<LinhaMunicipio> iterador = mapper.readerFor(LinhaMunicipio.class)
                .with(schemaLeitura)
                .readValues(config.arquivoTempCsv().toFile())) {
            linhasOriginais = iterador.readAll();
        } catch (JacksonException e) {
            logger.error("Erro ao ler CSV temporário para enriquecimento: {}", config.arquivoTempCsv(), e);
            throw new EleicaoDataProcessingException("Erro ao ler arquivo CSV temporário para enriquecimento: " + config.arquivoTempCsv(), e);
        }

        List<LinhaMunicipioEnriquecida> linhasEnriquecidas = linhasOriginais.parallelStream().map(linha -> {
            try {
                Optional<Metadados> dados = externalService.buscarMetadadosSecao(urlBuilder.buildUrlSecaoMetadados(linha));
                if (dados.isPresent() && dados.get().hashes() != null && !dados.get().hashes().isEmpty()) {
                    var hashInfo = dados.get().hashes().getFirst();
                    String buNome = hashInfo.arquivos() != null ? hashInfo.arquivos()
                            .stream()
                            .filter(arquivo -> TipoArquivoUrna.isArquivoUrna(arquivo.tipo()))
                            .map(Arquivo::nome)
                            .findFirst()
                            .orElse(StatusSecaoEleitoral.STATUS_INOPERANTE_AGREGADA) : StatusSecaoEleitoral.STATUS_INOPERANTE_AGREGADA;

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

                logger.warn(
                        "Metadados não encontrados para seção (HTTP 404): zona {}, seção {} do município {}. Marcada como {}.",
                        linha.numeroZona(), linha.numeroSecao(), linha.nomeMunicipio(), StatusSecaoEleitoral.STATUS_INOPERANTE_AGREGADA);

                return new LinhaMunicipioEnriquecida(
                        linha.unidadeFederativa(),
                        linha.codigoMunicipio(),
                        linha.nomeMunicipio(),
                        linha.numeroZona(),
                        linha.numeroSecao(),
                        StatusSecaoEleitoral.STATUS_INOPERANTE_AGREGADA,
                        StatusSecaoEleitoral.STATUS_INOPERANTE_AGREGADA);
            } catch (Exception e) {
                logger.error("Erro ao enriquecer linha do CSV: {}", linha, e);
                throw new EleicaoDataProcessingException("[enriquecer] - Erro ao enriquecer csv. Linha: " + linha, e);
            }
        }).toList();

        CsvSchema schemaEscrita = mapper.schemaFor(LinhaMunicipioEnriquecida.class).withHeader();
        try {
            if (!Files.exists(config.diretorioTemp())) {
                Files.createDirectories(config.diretorioTemp());
            }
            mapper.writer(schemaEscrita).writeValue(config.arquivoEnriquecidoCsv().toFile(), linhasEnriquecidas);
        } catch (IOException | JacksonException e) {
            logger.error("Erro ao gravar CSV enriquecido: {}", config.arquivoEnriquecidoCsv(), e);
            throw new EleicaoDataProcessingException("Erro ao salvar arquivo CSV enriquecido: " + config.arquivoEnriquecidoCsv(), e);
        }

        int totalInoperantes = (int) linhasEnriquecidas.stream()
                .filter(l -> StatusSecaoEleitoral.isInoperanteOuAgregada(l.boletimUrna()))
                .count();
        int totalAtivas = linhasEnriquecidas.size() - totalInoperantes;

        Duration decorrido = Duration.between(inicio, Instant.now());
        logger.info(
                "Enriquecimento concluído em {} ms. Total seções: {} (ativas: {}, inoperantes/agregadas: {}).",
                decorrido.toMillis(), linhasEnriquecidas.size(), totalAtivas, totalInoperantes);

        return new ResultadoEnriquecimento(linhasEnriquecidas.size(), totalAtivas, totalInoperantes);
    }

}
