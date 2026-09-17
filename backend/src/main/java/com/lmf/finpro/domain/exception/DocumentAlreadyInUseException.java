package com.lmf.finpro.domain.exception;

public class DocumentAlreadyInUseException extends RuntimeException {
    public DocumentAlreadyInUseException(String message) {
        super(message);
    }
}
