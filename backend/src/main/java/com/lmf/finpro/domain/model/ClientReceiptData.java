package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * Dados já resolvidos e validados para montar o recibo em PDF de um cliente num mês: quem emite
 * (usuário logado), quem recebe (cliente), o período e as receitas lançadas nele.
 */
public record ClientReceiptData(
        User issuer,
        Client client,
        YearMonth referenceMonth,
        List<Transaction> transactions,
        BigDecimal total) {}
