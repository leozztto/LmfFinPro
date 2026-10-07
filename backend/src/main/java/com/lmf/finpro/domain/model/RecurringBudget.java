package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de orçamento recorrente: a cada mês vencido gera um {@link Budget} real para a categoria.
 * {@code generatedMonths} conta quantos meses já foram lançados, e o próximo mês é derivado dele —
 * não há "próximo mês" persistido para ficar dessincronizado (mesma convenção de {@link
 * RecurringTransaction}).
 */
public record RecurringBudget(
        Long id,
        Long householdId,
        Long categoryId,
        BigDecimal limitValue,
        YearMonth startMonth,
        YearMonth endMonth,
        int generatedMonths,
        boolean active,
        LocalDateTime createdAt) {

    public static RecurringBudget create(
            Long householdId,
            Long categoryId,
            BigDecimal limitValue,
            YearMonth startMonth,
            YearMonth endMonth) {
        return new RecurringBudget(
                null,
                householdId,
                categoryId,
                limitValue,
                startMonth,
                endMonth,
                0,
                true,
                LocalDateTime.now());
    }

    public boolean belongsTo(Long candidateHouseholdId) {
        return householdId.equals(candidateHouseholdId);
    }

    /** Próximo mês ainda não lançado, ou {@code null} se a recorrência já terminou. */
    public YearMonth nextGenerationMonth() {
        YearMonth next = startMonth.plusMonths(generatedMonths);
        return endMonth != null && next.isAfter(endMonth) ? null : next;
    }

    /** Meses ainda não lançados até {@code currentMonth} (inclusive), em ordem. */
    public List<YearMonth> dueMonths(YearMonth currentMonth) {
        List<YearMonth> months = new ArrayList<>();
        if (!active) {
            return months;
        }
        int index = generatedMonths;
        YearMonth month = startMonth.plusMonths(index);
        while (!month.isAfter(currentMonth) && (endMonth == null || !month.isAfter(endMonth))) {
            months.add(month);
            index++;
            month = startMonth.plusMonths(index);
        }
        return months;
    }

    public RecurringBudget withGeneratedMonths(int newGeneratedMonths) {
        return new RecurringBudget(
                id,
                householdId,
                categoryId,
                limitValue,
                startMonth,
                endMonth,
                newGeneratedMonths,
                active,
                createdAt);
    }

    /**
     * Categoria e mês inicial não mudam depois de criada — alterá-los mudaria o significado dos
     * orçamentos já lançados. Ao reativar uma recorrência pausada, os meses que caíram durante a
     * pausa são pulados (pausar significa "não lançar", não "adiar").
     */
    public RecurringBudget withDetails(
            BigDecimal newLimitValue,
            YearMonth newEndMonth,
            boolean newActive,
            YearMonth currentMonth) {
        int months = generatedMonths;
        if (!active && newActive) {
            while (startMonth.plusMonths(months).isBefore(currentMonth)) {
                months++;
            }
        }
        return new RecurringBudget(
                id,
                householdId,
                categoryId,
                newLimitValue,
                startMonth,
                newEndMonth,
                months,
                newActive,
                createdAt);
    }
}
