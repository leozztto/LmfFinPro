package com.lmf.finpro.domain.exception;

/** Lançada ao tentar excluir um registro que ainda possui outros registros dependendo dele. */
public class EntityHasLinkedRecordsException extends RuntimeException {
    public EntityHasLinkedRecordsException(String message) {
        super(message);
    }
}
