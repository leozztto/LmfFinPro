package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Saldo devedor de uma dívida numa data. Vale até o próximo saldo informado; zero significa dívida
 * quitada. Um saldo por dívida e dia: informar de novo a mesma data substitui o valor.
 */
public record DebtBalance(
        Long id, Long debtId, LocalDate balanceDate, BigDecimal balance, LocalDateTime createdAt) {

    public static DebtBalance create(Long debtId, LocalDate balanceDate, BigDecimal balance) {
        return new DebtBalance(null, debtId, balanceDate, balance, LocalDateTime.now());
    }

    public DebtBalance withBalance(BigDecimal newBalance) {
        return new DebtBalance(id, debtId, balanceDate, newBalance, createdAt);
    }
}
