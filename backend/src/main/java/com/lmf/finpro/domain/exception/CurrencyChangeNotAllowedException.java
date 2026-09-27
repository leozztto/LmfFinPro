package com.lmf.finpro.domain.exception;

public class CurrencyChangeNotAllowedException extends RuntimeException {
    public CurrencyChangeNotAllowedException(String message) {
        super(message);
    }
}
