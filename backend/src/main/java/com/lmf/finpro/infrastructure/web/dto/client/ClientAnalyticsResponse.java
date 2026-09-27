package com.lmf.finpro.infrastructure.web.dto.client;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * @param topClientShare fração (0 a 1) da receita total que vem do maior cliente
 * @param risk NONE, LOW, MODERATE ou HIGH
 */
public record ClientAnalyticsResponse(
        @JsonFormat(pattern = "yyyy-MM") List<YearMonth> months,
        BigDecimal totalIncome,
        BigDecimal unassignedIncome,
        int activeClients,
        BigDecimal averageTicket,
        BigDecimal topClientShare,
        BigDecimal topThreeShare,
        String risk,
        List<ClientRow> ranking) {

    public record ClientRow(
            Long clientId,
            String name,
            String color,
            BigDecimal income,
            BigDecimal expense,
            BigDecimal net,
            int incomeCount,
            BigDecimal averageTicket,
            BigDecimal share,
            int activeMonths,
            LocalDate lastIncomeDate,
            List<MonthRow> monthly) {}

    public record MonthRow(
            @JsonFormat(pattern = "yyyy-MM") YearMonth month,
            BigDecimal income,
            BigDecimal expense) {}
}
