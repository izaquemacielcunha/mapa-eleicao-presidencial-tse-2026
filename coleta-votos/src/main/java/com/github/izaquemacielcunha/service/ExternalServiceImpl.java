package com.github.izaquemacielcunha.service;

import java.io.InputStream;
import java.time.Duration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.github.izaquemacielcunha.model.urnas.Metadados;
import com.github.izaquemacielcunha.model.Municipio;
import io.github.resilience4j.ratelimiter.RateLimiter;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.ObjectMapper;

@Singleton
public class ExternalServiceImpl {
    public static final String MUNICIPIO = "mu";

    private final HttpClient httpClient;
    private final RateLimiter rateLimiter;
    private final ObjectMapper mapper;

    @Inject
    public ExternalServiceImpl(HttpClient httpClient, RateLimiter rateLimiter, ObjectMapper mapper) {
        this.httpClient = httpClient;
        this.rateLimiter = rateLimiter;
        this.mapper = mapper;
    }

    public Optional<Municipio> callMunicipioConfig(String url, String codigoMunicipal) throws Exception {
        if (Objects.isNull(url) || url.trim().isEmpty() || Objects.isNull(codigoMunicipal) || codigoMunicipal.trim().isEmpty()) {
            return Optional.empty();
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET()
                .build();

        InputStream responseBody = RateLimiter.decorateSupplier(rateLimiter, () -> {
            try {
                HttpResponse<InputStream> response = httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofInputStream());
                return response.body();
            }
            catch (Exception e) {
                throw new RuntimeException("[callMunicipioConfig] - Erro ao executar chamada HTTP", e);
            }
        }).get();

        return extrairMunicipio(responseBody, codigoMunicipal);
    }

    public Optional<Metadados> callZonaSecaoInfos(String url) throws Exception {
        if (Objects.isNull(url) || url.trim().isEmpty()) {
            return Optional.empty();
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET()
                .build();

        InputStream responseBody = RateLimiter.decorateSupplier(rateLimiter, () -> {
            try {
                HttpResponse<InputStream> response = httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofInputStream());
                return response.body();
            }
            catch (Exception e) {
                throw new RuntimeException("[callMunicipioConfig] - Erro ao executar chamada HTTP", e);
            }
        }).get();

        return Optional.ofNullable(mapper.readValue(responseBody, Metadados.class));
    }

    private Optional<Municipio> extrairMunicipio(InputStream jsonStream, String codigoDesejado) {
        if (jsonStream == null || codigoDesejado == null || codigoDesejado.trim().isEmpty()) {
            return Optional.empty();
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
        }
        catch (Exception e) {
            throw new RuntimeException("Erro de I/O ao ler o stream do JSON via parser", e);
        }
        return Optional.empty();
    }
}// end of class
