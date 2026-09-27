package com.lmf.finpro.domain.exception;

public class ExchangeRateUnavailableException extends RuntimeException {
    public ExchangeRateUnavailableException(String message) {
        super(message);
    }
}
