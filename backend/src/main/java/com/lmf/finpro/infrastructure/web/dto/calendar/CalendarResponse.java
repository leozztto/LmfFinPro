package com.lmf.finpro.infrastructure.web.dto.calendar;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * @param expectedIncome receitas em aberto no mês (pendentes, atrasadas e previstas)
 * @param expectedExpense despesas em aberto no mês (pendentes, atrasadas, previstas e DAS)
 * @param days só os dias do mês com algum lançamento
 * @param overdue pendentes com data anterior a hoje, de qualquer mês
 */
public record CalendarResponse(
        @JsonFormat(pattern = "yyyy-MM") YearMonth month,
        LocalDate today,
        BigDecimal expectedIncome,
        BigDecimal expectedExpense,
        BigDecimal paidIncome,
        BigDecimal paidExpense,
        BigDecimal overdueIncome,
        BigDecimal overdueExpense,
        List<DayRow> days,
        List<EntryRow> overdue) {

    public record DayRow(
            LocalDate date, BigDecimal income, BigDecimal expense, List<EntryRow> entries) {}

    /**
     * @param kind TRANSACTION, RECURRING_FORECAST ou DAS
     * @param status PAID, PENDING, OVERDUE ou FORECAST
     * @param amount {@code null} só no DAS sem estimativa de imposto da competência
     * @param competence só no DAS
     */
    public record EntryRow(
            String kind,
            LocalDate date,
            String description,
            BigDecimal amount,
            String type,
            String status,
            Long transactionId,
            Long recurringTransactionId,
            String accountName,
            String categoryName,
            String clientName,
            @JsonFormat(pattern = "yyyy-MM") YearMonth competence) {}
}
