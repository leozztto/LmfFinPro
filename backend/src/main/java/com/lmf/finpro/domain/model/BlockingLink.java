package com.lmf.finpro.domain.model;

/**
 * Vínculo que impede compartilhar uma conta sozinha: uma meta de economia que liga a conta a outra
 * que ficaria no espaço pessoal (a meta pertence a um grupo só). Transferências não entram aqui:
 * quando cruzam a fronteira, são divididas entre os dois espaços.
 *
 * @param accountName a conta que seria compartilhada
 * @param otherAccountName a conta do outro lado, que não está sendo compartilhada
 */
public record BlockingLink(Type type, String accountName, String otherAccountName) {

    public enum Type {
        SAVINGS_GOAL
    }
}
