package com.lmf.finpro.domain.exception;

public class TagAlreadyExistsException extends RuntimeException {
    public TagAlreadyExistsException(String message) {
        super(message);
    }
}
