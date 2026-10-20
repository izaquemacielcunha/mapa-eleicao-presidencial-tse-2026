package com.github.izaquemacielcunha.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EleicaoExceptionTest {

    @Test
    @DisplayName("Deve instanciar EleicaoIntegrationException com mensagem e causa")
    void deveInstanciarIntegrationException() {
        Throwable cause = new RuntimeException("Causa raiz");
        EleicaoIntegrationException ex = new EleicaoIntegrationException("Erro de rede", cause);

        assertEquals("Erro de rede", ex.getMessage());
        assertEquals(cause, ex.getCause());

        EleicaoIntegrationException exMsgOnly = new EleicaoIntegrationException("Erro simples");
        assertEquals("Erro simples", exMsgOnly.getMessage());

        EleicaoIntegrationException exCauseOnly = new EleicaoIntegrationException(cause);
        assertEquals(cause, exCauseOnly.getCause());
    }

    @Test
    @DisplayName("Deve instanciar EleicaoDataProcessingException com mensagem e causa")
    void deveInstanciarDataProcessingException() {
        Throwable cause = new RuntimeException("Causa raiz de arquivo");
        EleicaoDataProcessingException ex = new EleicaoDataProcessingException("Erro de arquivo", cause);

        assertEquals("Erro de arquivo", ex.getMessage());
        assertEquals(cause, ex.getCause());

        EleicaoDataProcessingException exMsgOnly = new EleicaoDataProcessingException("Erro simples");
        assertEquals("Erro simples", exMsgOnly.getMessage());

        EleicaoDataProcessingException exCauseOnly = new EleicaoDataProcessingException(cause);
        assertEquals(cause, exCauseOnly.getCause());
    }
}
