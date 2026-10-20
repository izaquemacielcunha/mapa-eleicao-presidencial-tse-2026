package com.github.izaquemacielcunha.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.izaquemacielcunha.exception.EleicaoDataProcessingException;
import com.github.izaquemacielcunha.exception.EleicaoIntegrationException;
import com.github.izaquemacielcunha.model.Municipio;
import com.github.izaquemacielcunha.model.urna.Metadados;
import io.github.resilience4j.ratelimiter.RateLimiter;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.ObjectMapper;

@Singleton
public class ExternalServiceImpl implements ExternalService {
    private static final Logger logger = LoggerFactory.getLogger(ExternalServiceImpl.class);
    private static final String MUNICIPIO = "mu";
    private static final int SUCCESS = 200;
    private static final int NOT_FOUND = 404;

    private final HttpClient httpClient;
    private final RateLimiter rateLimiter;
    private final ObjectMapper mapper;

    @Inject
    public ExternalServiceImpl(HttpClient httpClient, RateLimiter rateLimiter, ObjectMapper mapper) {
        this.httpClient = httpClient;
        this.rateLimiter = rateLimiter;
        this.mapper = mapper;
    }

    @Override
    public Optional<Municipio> buscarConfiguracaoMunicipio(String url, String codigoMunicipal) {
        Objects.requireNonNull(url, "URL não pode ser nula");
        if (url.isBlank()) {
            throw new IllegalArgumentException("URL não pode ser vazia");
        }
        Objects.requireNonNull(codigoMunicipal, "Código municipal não pode ser nulo");
        if (codigoMunicipal.isBlank()) {
            throw new IllegalArgumentException("Código municipal não pode ser vazio");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET()
                .build();

        return executarRequisicaoComRateLimit(
                request,
                HttpResponse.BodyHandlers.ofInputStream(),
                response -> {
                    try (InputStream body = response.body()) {
                        return extrairMunicipio(body, codigoMunicipal);
                    } catch (IOException e) {
                        logger.error("Erro ao processar stream JSON do município: {}", url, e);
                        throw new EleicaoIntegrationException("Erro ao processar resposta do município: " + url, e);
                    }
                }
        );
    }

    @Override
    public Optional<Metadados> buscarMetadadosSecao(String url) {
        Objects.requireNonNull(url, "URL não pode ser nula");
        if (url.isBlank()) {
            throw new IllegalArgumentException("URL não pode ser vazia");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET()
                .build();

        return executarRequisicaoComRateLimit(
                request,
                HttpResponse.BodyHandlers.ofInputStream(),
                response -> {
                    try (InputStream body = response.body()) {
                        return Optional.ofNullable(mapper.readValue(body, Metadados.class));
                    } catch (IOException | JacksonException e) {
                        logger.error("Erro ao desserializar metadados JSON da seção: {}", url, e);
                        throw new EleicaoIntegrationException("Erro ao desserializar metadados da seção: " + url, e);
                    }
                }
        );
    }

    @Override
    public Optional<Path> baixarArquivoBoletimUrnaDat(String url, Path diretorioDestino) {
        Objects.requireNonNull(url, "URL não pode ser nula");
        if (url.isBlank()) {
            throw new IllegalArgumentException("URL não pode ser vazia");
        }
        Objects.requireNonNull(diretorioDestino, "Diretório de destino não pode ser nulo");

        Path caminhoDestino = resolverDiretorioArquivo(url, diretorioDestino);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        try {
            return executarRequisicaoComRateLimit(
                    request,
                    HttpResponse.BodyHandlers.ofFile(caminhoDestino),
                    response -> Optional.ofNullable(response.body())
            );
        } catch (Exception e) {
            try {
                Files.deleteIfExists(caminhoDestino);
            } catch (IOException ignored) {}
            throw e;
        }
    }

    private <T, R> Optional<R> executarRequisicaoComRateLimit(
            HttpRequest request,
            HttpResponse.BodyHandler<T> bodyHandler,
            Function<HttpResponse<T>, Optional<R>> responseHandler
    ) {
        return RateLimiter.decorateSupplier(rateLimiter, () -> {
            try {
                HttpResponse<T> response = httpClient.send(request, bodyHandler);

                if (response.statusCode() == NOT_FOUND) {
                    logger.warn("Recurso não encontrado (HTTP 404) no TSE: {}", request.uri());
                    if (response.body() instanceof Path path) {
                        Files.deleteIfExists(path);
                    }
                    return Optional.<R>empty();
                }

                if (response.statusCode() != SUCCESS) {
                    logger.error("Status HTTP inesperado retornado pelo TSE. Status: {} - URL: {}", response.statusCode(), request.uri());
                    if (response.body() instanceof Path path) {
                        Files.deleteIfExists(path);
                    }
                    throw new EleicaoIntegrationException("Erro na resposta HTTP. Status: " + response.statusCode() + " - URL: " + request.uri());
                }

                return responseHandler.apply(response);
            } catch (InterruptedException e) {
                logger.error("Requisição HTTP ao TSE interrompida: {}", request.uri(), e);
                Thread.currentThread().interrupt();
                throw new EleicaoIntegrationException("Requisição HTTP interrompida: " + request.uri(), e);
            } catch (IOException e) {
                logger.error("Falha de rede/I/O não recuperável na requisição: {}", request.uri(), e);
                throw new EleicaoIntegrationException("Erro de I/O na chamada HTTP: " + request.uri(), e);
            } catch (EleicaoIntegrationException e) {
                throw e;
            } catch (Exception e) {
                logger.error("Erro inesperado na chamada ao TSE: {}", request.uri(), e);
                throw new EleicaoIntegrationException("Erro ao executar chamada HTTP: " + request.uri(), e);
            }
        }).get();
    }

    private Optional<Municipio> extrairMunicipio(InputStream jsonStream, String codigoDesejado) {
        Objects.requireNonNull(jsonStream, "InputStream do JSON não pode ser nulo");
        Objects.requireNonNull(codigoDesejado, "Código desejado não pode ser nulo");
        if (codigoDesejado.isBlank()) {
            throw new IllegalArgumentException("Código desejado não pode ser vazio");
        }

        try (JsonParser parser = mapper.tokenStreamFactory().createParser(jsonStream)) {
            while (Objects.nonNull(parser.nextToken())) {
                if (parser.currentToken() != JsonToken.PROPERTY_NAME || !MUNICIPIO.equals(parser.currentName())) {
                    continue;
                }
                if (parser.nextToken() != JsonToken.START_ARRAY) {
                    continue;
                }
                while (parser.nextToken() != JsonToken.END_ARRAY) {
                    if (parser.currentToken() == JsonToken.START_OBJECT) {
                        Municipio municipio = mapper.readValue(parser, Municipio.class);
                        if (codigoDesejado.equals(municipio.codigo())) {
                            return Optional.of(municipio);
                        }
                    }
                }
                return Optional.empty();
            }
        } catch (Exception e) {
            logger.error("Erro no parsing do payload JSON do TSE", e);
            throw new EleicaoIntegrationException("Erro de parsing/leitura do stream JSON do TSE", e);
        }
        return Optional.empty();
    }

    private Path resolverDiretorioArquivo(String url, Path diretorioDestino) {
        String nomeArquivo = url.substring(url.lastIndexOf('/') + 1);
        Path caminhoDestino = diretorioDestino.resolve(nomeArquivo);

        try {
            if (!Files.exists(diretorioDestino)) {
                Files.createDirectories(diretorioDestino);
            }
        } catch (IOException e) {
            logger.error("Erro ao criar diretório de destino de arquivos: {}", diretorioDestino, e);
            throw new EleicaoDataProcessingException("Erro ao criar diretório de destino: " + diretorioDestino, e);
        }

        return caminhoDestino;
    }
}
