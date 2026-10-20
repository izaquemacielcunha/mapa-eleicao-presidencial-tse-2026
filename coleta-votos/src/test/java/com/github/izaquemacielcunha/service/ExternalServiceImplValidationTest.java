package com.github.izaquemacielcunha.service;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExternalServiceImplValidationTest {

    private ExternalServiceImpl externalService;

    @BeforeEach
    void setUp() {
        externalService = new ExternalServiceImpl(null, null, null);
    }

    @Test
    @DisplayName("buscarConfiguracaoMunicipio deve lançar NullPointerException para parâmetros nulos")
    void buscarConfiguracaoMunicipioValidacaoNulos() {
        assertThrows(NullPointerException.class, () -> externalService.buscarConfiguracaoMunicipio(null, "75353"));
        assertThrows(NullPointerException.class, () -> externalService.buscarConfiguracaoMunicipio("https://example.com", null));
    }

    @Test
    @DisplayName("buscarConfiguracaoMunicipio deve lançar IllegalArgumentException para parâmetros em branco")
    void buscarConfiguracaoMunicipioValidacaoBrancos() {
        assertThrows(IllegalArgumentException.class, () -> externalService.buscarConfiguracaoMunicipio("   ", "75353"));
        assertThrows(IllegalArgumentException.class, () -> externalService.buscarConfiguracaoMunicipio("https://example.com", "   "));
    }

    @Test
    @DisplayName("buscarMetadadosSecao deve lançar exceção para URL nula ou em branco")
    void buscarMetadadosSecaoValidacao() {
        assertThrows(NullPointerException.class, () -> externalService.buscarMetadadosSecao(null));
        assertThrows(IllegalArgumentException.class, () -> externalService.buscarMetadadosSecao("   "));
    }

    @Test
    @DisplayName("Métodos legados callMunicipioConfig e callZonaSecaoInfos devem delegar e validar")
    @SuppressWarnings("deprecation")
    void metodosLegadosValidacao() {
        assertThrows(NullPointerException.class, () -> externalService.callMunicipioConfig(null, "75353"));
        assertThrows(NullPointerException.class, () -> externalService.callZonaSecaoInfos(null));
    }

    @Test
    @DisplayName("baixarArquivoBoletimUrnaDat deve lançar exceção para parâmetros nulos ou em branco")
    void baixarArquivoBoletimUrnaDatValidacao() {
        Path tempDir = Path.of("temp-files");
        assertThrows(NullPointerException.class, () -> externalService.baixarArquivoBoletimUrnaDat(null, tempDir));
        assertThrows(IllegalArgumentException.class, () -> externalService.baixarArquivoBoletimUrnaDat("   ", tempDir));
        assertThrows(NullPointerException.class, () -> externalService.baixarArquivoBoletimUrnaDat("https://example.com", null));
    }
}
