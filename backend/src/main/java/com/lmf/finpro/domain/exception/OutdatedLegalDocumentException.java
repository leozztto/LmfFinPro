package com.lmf.finpro.domain.exception;

/** O aceite se refere a uma versão dos termos ou da política que não é mais a vigente. */
public class OutdatedLegalDocumentException extends RuntimeException {
    public OutdatedLegalDocumentException(String message) {
        super(message);
    }
}
