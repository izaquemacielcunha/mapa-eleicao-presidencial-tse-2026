package com.github.izaquemacielcunha.exception;

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
