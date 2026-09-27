package com.lmf.finpro.domain.exception;

/** Anexo recusado: vazio, grande demais, formato não aceito ou limite de anexos atingido. */
public class AttachmentInvalidException extends RuntimeException {
    public AttachmentInvalidException(String message) {
        super(message);
    }
}
