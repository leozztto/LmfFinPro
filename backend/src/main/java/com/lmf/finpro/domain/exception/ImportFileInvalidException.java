package com.lmf.finpro.domain.exception;

/**
 * Lançada quando o arquivo enviado para importação está vazio, ilegível ou fora do formato
 * esperado.
 */
public class ImportFileInvalidException extends RuntimeException {
    public ImportFileInvalidException(String message) {
        super(message);
    }
}
