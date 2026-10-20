package com.github.izaquemacielcunha.exception;

/**
 * Exceção não checada (unchecked) para erros de integração com serviços externos,
 * incluindo falhas de comunicação HTTP/rede e parsing de payloads retornados pelo TSE.
 */
public class EleicaoIntegrationException extends RuntimeException {

    public EleicaoIntegrationException(String message) {
        super(message);
    }

    public EleicaoIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }

    public EleicaoIntegrationException(Throwable cause) {
        super(cause);
    }
}
