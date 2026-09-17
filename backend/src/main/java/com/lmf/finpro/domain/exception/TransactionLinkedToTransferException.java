package com.lmf.finpro.domain.exception;

public class TransactionLinkedToTransferException extends RuntimeException {
    public TransactionLinkedToTransferException(String message) {
        super(message);
    }
}
