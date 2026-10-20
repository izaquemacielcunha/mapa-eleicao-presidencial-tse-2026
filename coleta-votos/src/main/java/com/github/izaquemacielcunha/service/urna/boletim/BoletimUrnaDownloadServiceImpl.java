package com.github.izaquemacielcunha.service.urna.boletim;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

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
import com.github.izaquemacielcunha.model.urna.StatusSecaoEleitoral;
import com.github.izaquemacielcunha.service.ExternalService;
import com.github.izaquemacielcunha.service.tse.TseUrlBuilder;

@Singleton
public class BoletimUrnaDownloadServiceImpl implements BoletimUrnaDownloadService {
    private static final Logger logger = LoggerFactory.getLogger(BoletimUrnaDownloadServiceImpl.class);

    public record ResultadoDownload(int totalSolicitados, int totalBaixados, int totalInoperantesIgnorados) {}

    private final ExternalService externalService;
    private final CsvMapper mapper;
    private final TseUrlBuilder urlBuilder;
    private final EleicaoConfig config;

    @Inject
    public BoletimUrnaDownloadServiceImpl(
            ExternalService externalService,
            CsvMapper mapper,
            TseUrlBuilder urlBuilder,
            EleicaoConfig config
    ) {
        this.externalService = Objects.requireNonNull(externalService, "ExternalService não pode ser nulo");
        this.mapper = Objects.requireNonNull(mapper, "CsvMapper não pode ser nulo");
        this.urlBuilder = Objects.requireNonNull(urlBuilder, "TseUrlBuilder não pode ser nulo");
        this.config = Objects.requireNonNull(config, "EleicaoConfig não pode ser nulo");
    }

    @Override
    public void baixarBoletinsUrna() {
        baixarBoletinsUrna(config.arquivoEnriquecidoCsv(), config.diretorioBoletins());
    }

    private ResultadoDownload baixarBoletinsUrna(Path arquivoEnriquecido, Path diretorioDestino) {
        Objects.requireNonNull(arquivoEnriquecido, "Arquivo enriquecido não pode ser nulo");
        return baixarBoletinsUrna(arquivoEnriquecido.toFile(), diretorioDestino);
    }

    private ResultadoDownload baixarBoletinsUrna(File arquivoEnriquecido, Path diretorioDestino) {
        Objects.requireNonNull(arquivoEnriquecido, "Arquivo enriquecido não pode ser nulo");
        Objects.requireNonNull(diretorioDestino, "Diretório de destino não pode ser nulo");

        Instant inicio = Instant.now();

        CsvSchema schemaLeitura = mapper.schemaFor(LinhaMunicipioEnriquecida.class).withHeader();
        List<LinhaMunicipioEnriquecida> linhasOriginais;
        try (MappingIterator<LinhaMunicipioEnriquecida> iterador = mapper.readerFor(LinhaMunicipioEnriquecida.class)
                .with(schemaLeitura)
                .readValues(arquivoEnriquecido)) {
            linhasOriginais = iterador.readAll();
        } catch (JacksonException e) {
            logger.error("Erro ao ler CSV enriquecido: {}", arquivoEnriquecido, e);
            throw new EleicaoDataProcessingException("Erro ao ler arquivo enriquecido: " + arquivoEnriquecido, e);
        }

        AtomicInteger totalBaixados = new AtomicInteger(0);
        AtomicInteger totalInoperantes = new AtomicInteger(0);

        linhasOriginais.parallelStream().forEach(linha -> {
            try {
                if (StatusSecaoEleitoral.isInoperanteOuAgregada(linha.boletimUrna())) {
                    totalInoperantes.incrementAndGet();
                    return;
                }

                String urlBu = urlBuilder.buildUrlBoletimUrna(linha);
                Optional<Path> arquivoBaixado = externalService.baixarArquivoBoletimUrnaDat(urlBu, diretorioDestino);

                if (arquivoBaixado.isPresent()) {
                    totalBaixados.incrementAndGet();
                } else {
                    logger.warn("Arquivo .dat do boletim de urna não encontrado (404) no TSE: {}", linha.boletimUrna());
                }
            } catch (Exception e) {
                logger.error("Erro ao baixar arquivo do boletim: {}", linha.boletimUrna(), e);
                throw new EleicaoDataProcessingException("Erro ao baixar boletim da urna: " + linha.boletimUrna(), e);
            }
        });

        Duration decorrido = Duration.between(inicio, Instant.now());
        logger.info(
                "Download de boletins concluído em {} ms. Total seções: {} (baixados: {}, inoperantes ignorados: {}).",
                decorrido.toMillis(), linhasOriginais.size(), totalBaixados.get(), totalInoperantes.get());

        return new ResultadoDownload(linhasOriginais.size(), totalBaixados.get(), totalInoperantes.get());
    }
}
