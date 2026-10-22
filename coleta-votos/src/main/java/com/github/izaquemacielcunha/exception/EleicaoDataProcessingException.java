package com.github.izaquemacielcunha.exception;

public class EleicaoDataProcessingException extends RuntimeException {

    public EleicaoDataProcessingException(String message) {
        super(message);
    }

    public EleicaoDataProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public EleicaoDataProcessingException(Throwable cause) {
        super(cause);
    }
}
