package com.lmf.finpro.domain.exception;

public class CepServiceUnavailableException extends RuntimeException {
    public CepServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
