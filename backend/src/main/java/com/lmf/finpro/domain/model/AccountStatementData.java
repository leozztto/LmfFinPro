package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * Dados já resolvidos e validados para montar o extrato em PDF de uma conta num mês: titular
 * (usuário logado), a conta, o saldo de abertura do período (saldo inicial da conta + tudo antes
 * do período), as transações lançadas dentro do período e os totais/saldo de fechamento.
 */
public record AccountStatementData(
        User issuer,
        Account account,
        YearMonth referenceMonth,
        BigDecimal openingBalance,
        List<Transaction> transactions,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal closingBalance) {}
