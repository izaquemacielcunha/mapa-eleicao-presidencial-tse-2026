package com.github.izaquemacielcunha.service.tse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import io.github.resilience4j.ratelimiter.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

import com.github.izaquemacielcunha.dependencyinjection.MapperModule;
import com.github.izaquemacielcunha.exception.EleicaoIntegrationException;
import com.github.izaquemacielcunha.model.territorio.Municipio;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExternalServiceImplTest {

    private HttpClient httpClient;
    private ExternalServiceImpl service;

    @BeforeEach
    void setUp() {
        httpClient = mock(HttpClient.class);
        RateLimiter rateLimiter = RateLimiter.ofDefaults("testLimiter");
        ObjectMapper mapper = MapperModule.provideObjectMapper();
        service = new ExternalServiceImpl(httpClient, rateLimiter, mapper);
    }

    @Test
    @DisplayName("Deve extrair município com streaming JSON do TSE com sucesso")
    @SuppressWarnings("unchecked")
    void deveExtrairMunicipioComSucesso() throws Exception {
        String jsonPayload = """
                {
                  "mu": [
                    { "cd": "75353", "nm": "CURITIBA", "z": [] },
                    { "cd": "75354", "nm": "LONDRINA", "z": [] }
                  ]
                }
                """;
        HttpResponse<InputStream> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(new ByteArrayInputStream(jsonPayload.getBytes(StandardCharsets.UTF_8)));

        doReturn(response).when(httpClient).send(any(HttpRequest.class), any());

        Optional<Municipio> municipio = service.buscarConfiguracaoMunicipio("https://tse.jus.br/config.json", "75353");

        assertTrue(municipio.isPresent());
        assertEquals("75353", municipio.get().codigo());
        assertEquals("CURITIBA", municipio.get().nome());
    }

    @Test
    @DisplayName("Deve retornar Optional.empty() quando status HTTP for 404")
    @SuppressWarnings("unchecked")
    void deveRetornarVazioEm404() throws Exception {
        HttpResponse<InputStream> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(404);

        doReturn(response).when(httpClient).send(any(HttpRequest.class), any());

        Optional<Municipio> municipio = service.buscarConfiguracaoMunicipio("https://tse.jus.br/config.json", "75353");
        assertTrue(municipio.isEmpty());
    }

    @Test
    @DisplayName("Deve lançar EleicaoIntegrationException quando status HTTP for 500")
    @SuppressWarnings("unchecked")
    void deveLancarExcecaoEmHttp500() throws Exception {
        HttpResponse<InputStream> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(500);

        doReturn(response).when(httpClient).send(any(HttpRequest.class), any());

        assertThrows(EleicaoIntegrationException.class, () ->
                service.buscarConfiguracaoMunicipio("https://tse.jus.br/config.json", "75353"));
    }

    @Test
    @DisplayName("Deve excluir arquivo parcial quando download de arquivo falhar")
    @SuppressWarnings("unchecked")
    void deveExcluirArquivoParcialQuandoDownloadFalhar(@TempDir Path tempDir) throws Exception {
        Path arquivoDestino = tempDir.resolve("o00407-7535300010001.dat");
        Files.writeString(arquivoDestino, "conteudo parcial corrompido");
        assertTrue(Files.exists(arquivoDestino));

        HttpResponse<Path> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(500);
        when(response.body()).thenReturn(arquivoDestino);

        doReturn(response).when(httpClient).send(any(HttpRequest.class), any());

        assertThrows(EleicaoIntegrationException.class, () ->
                service.baixarArquivoBoletimUrnaDat("https://tse.jus.br/o00407-7535300010001.dat", tempDir));

        assertFalse(Files.exists(arquivoDestino), "O arquivo parcial deve ser excluído após falha no download");
    }
}
