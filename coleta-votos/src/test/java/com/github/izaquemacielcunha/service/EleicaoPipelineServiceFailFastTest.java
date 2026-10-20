package com.github.izaquemacielcunha.service;

import java.nio.file.Path;

import com.github.izaquemacielcunha.config.EleicaoConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EleicaoPipelineServiceFailFastTest {

    @Test
    @DisplayName("executar deve lançar IllegalStateException quando arquivo de coordenadas do TSE não existe")
    void executarDeveFalharQuandoArquivoCoordenadasNaoExiste() {
        EleicaoConfig configInexistente = new EleicaoConfig(
                "ZZ",
                "99999",
                "3220",
                "ele2026",
                Path.of("temp-files-inexistente"),
                Path.of("temp-files-inexistente/boletins")
        );

        EleicaoPipelineService service = new EleicaoPipelineServiceImpl(
                configInexistente,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, service::executar);
        assertTrue(exception.getMessage().contains("Arquivo de coordenadas do TSE não encontrado"));
    }
}
