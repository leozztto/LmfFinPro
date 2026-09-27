package com.lmf.finpro.infrastructure.web.dto.account;

import com.lmf.finpro.domain.model.AccountScope;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.Currency;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @param currentBalance saldo atual, na moeda da conta
 * @param currentBalanceInBrl saldo atual em reais pela última cotação; {@code null} se a cotação
 *     estiver indisponível
 * @param hasEntries a conta já tem lançamentos — a moeda não pode mais ser trocada
 */
public record AccountResponse(
        Long id,
        String name,
        AccountType type,
        BigDecimal initialBalance,
        BigDecimal currentBalance,
        LocalDateTime createdAt,
        AccountScope scope,
        Currency currency,
        BigDecimal currentBalanceInBrl,
        boolean hasEntries) {}
