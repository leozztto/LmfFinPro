package com.lmf.finpro.application.report;

import com.lmf.finpro.domain.model.AccountScope;
import com.lmf.finpro.domain.model.TransactionStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Filtros opcionais dos relatórios de receitas e de despesas — qualquer um pode ser nulo. */
public record TransactionReportFilters(
        LocalDate startDate,
        LocalDate endDate,
        Long accountId,
        AccountScope accountScope,
        Long categoryId,
        Long clientId,
        TransactionStatus status,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        String description) {}
