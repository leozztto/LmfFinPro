package com.lmf.finpro.infrastructure.web.exception;

import lombok.Getter;

/**
 * Limite de tentativas excedido; vira HTTP 429 com Retry-After no {@link GlobalExceptionHandler}.
 */
@Getter
public class TooManyRequestsException extends RuntimeException {

    private final long retryAfterSeconds;

    public TooManyRequestsException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
