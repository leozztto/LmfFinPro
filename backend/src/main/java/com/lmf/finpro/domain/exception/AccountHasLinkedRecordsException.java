package com.lmf.finpro.domain.exception;

public class AccountHasLinkedRecordsException extends RuntimeException {
    public AccountHasLinkedRecordsException(String message) {
        super(message);
    }
}
