package com.lmf.finpro.infrastructure.web.dto.debt;

import com.lmf.finpro.domain.model.DebtType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @param currentBalance último saldo devedor informado
 * @param lastBalanceDate data desse saldo
 */
public record DebtResponse(
        Long id,
        String name,
        DebtType type,
        String creditor,
        BigDecimal currentBalance,
        LocalDate lastBalanceDate) {}
