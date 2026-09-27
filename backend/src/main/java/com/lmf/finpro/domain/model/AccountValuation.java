package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Valor de mercado de uma conta de investimento no fim do dia {@code valuationDate} (ex.: o saldo
 * do extrato da corretora), já considerando todas as movimentações com data até esse dia. Uma
 * valorização por conta e dia: informar de novo a mesma data substitui o valor.
 */
public record AccountValuation(
        Long id,
        Long accountId,
        LocalDate valuationDate,
        BigDecimal value,
        LocalDateTime createdAt) {

    public static AccountValuation create(
            Long accountId, LocalDate valuationDate, BigDecimal value) {
        return new AccountValuation(null, accountId, valuationDate, value, LocalDateTime.now());
    }

    /**
     * Novo valor para a mesma data. O momento do registro também é atualizado: o valor passa a
     * incluir o que foi lançado até agora no dia.
     */
    public AccountValuation withValue(BigDecimal newValue) {
        return new AccountValuation(id, accountId, valuationDate, newValue, LocalDateTime.now());
    }
}
