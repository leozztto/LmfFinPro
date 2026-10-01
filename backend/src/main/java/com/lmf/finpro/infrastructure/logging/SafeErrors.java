package com.lmf.finpro.infrastructure.logging;

import org.springframework.core.NestedExceptionUtils;

/**
 * Descreve uma exceção para o log sem a mensagem: erros de banco e de SMTP costumam trazer valores
 * de linha e endereços de e-mail, e o stack trace repete essa mensagem. Fica só o tipo e o tipo da
 * causa raiz, o bastante para diagnosticar junto com os ids que o log já traz.
 */
public final class SafeErrors {

    private SafeErrors() {}

    public static String describe(Throwable ex) {
        Throwable root = NestedExceptionUtils.getMostSpecificCause(ex);
        String type = ex.getClass().getSimpleName();
        return root == ex ? type : type + " (causa: " + root.getClass().getSimpleName() + ")";
    }
}
