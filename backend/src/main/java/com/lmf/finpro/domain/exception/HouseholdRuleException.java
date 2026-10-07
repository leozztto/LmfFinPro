package com.lmf.finpro.domain.exception;

/** Operação que o estado do grupo não permite (ex.: dono sair sem transferir a posse). */
public class HouseholdRuleException extends RuntimeException {
    public HouseholdRuleException(String message) {
        super(message);
    }
}
