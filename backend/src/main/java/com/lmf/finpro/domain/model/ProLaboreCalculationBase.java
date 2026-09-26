package com.lmf.finpro.domain.model;

/** De onde parte o cálculo do pró-labore. */
public enum ProLaboreCalculationBase {
    /** O que a empresa gerou no mês: receitas recebidas menos custos, imposto e reserva. */
    MONTH_INCOME,
    /** O que dá para tirar do caixa: saldo PJ menos contas a pagar, imposto e colchão. */
    CURRENT_BALANCE
}
