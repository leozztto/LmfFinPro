package com.lmf.finpro.domain.model;

public enum ActivationEmailKind {
    /** Boas-vindas, logo após o cadastro. */
    WELCOME,
    /** No dia seguinte, para quem ainda não trouxe nenhum lançamento. */
    FIRST_IMPORT_REMINDER,
    /** Uma semana depois, último lembrete, para quem segue sem lançamentos. */
    WEEK_ONE_CHECK_IN
}
