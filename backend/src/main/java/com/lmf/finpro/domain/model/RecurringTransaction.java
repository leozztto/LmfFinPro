package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de lançamento recorrente: a cada ocorrência vencida vira uma {@link Transaction} real.
 * {@code generatedOccurrences} conta quantas ocorrências já foram lançadas, e a próxima data é
 * derivada dela — não há "próxima data" persistida para ficar dessincronizada.
 */
public record RecurringTransaction(
        Long id,
        Long userId,
        Long accountId,
        Long categoryId,
        Long clientId,
        String description,
        BigDecimal amount,
        CategoryType type,
        RecurrenceFrequency frequency,
        LocalDate startDate,
        LocalDate endDate,
        int generatedOccurrences,
        boolean active,
        LocalDateTime createdAt) {

    public static RecurringTransaction create(
            Long userId,
            Long accountId,
            Long categoryId,
            Long clientId,
            String description,
            BigDecimal amount,
            CategoryType type,
            RecurrenceFrequency frequency,
            LocalDate startDate,
            LocalDate endDate) {
        return new RecurringTransaction(
                null,
                userId,
                accountId,
                categoryId,
                clientId,
                description,
                amount,
                type,
                frequency,
                startDate,
                endDate,
                0,
                true,
                LocalDateTime.now());
    }

    public boolean belongsTo(Long candidateUserId) {
        return userId.equals(candidateUserId);
    }

    /** Próxima ocorrência ainda não lançada, ou {@code null} se a recorrência já terminou. */
    public LocalDate nextOccurrenceDate() {
        LocalDate next = frequency.occurrence(startDate, generatedOccurrences);
        return endDate != null && next.isAfter(endDate) ? null : next;
    }

    /** Datas das ocorrências ainda não lançadas até {@code today} (inclusive), em ordem. */
    public List<LocalDate> dueOccurrenceDates(LocalDate today) {
        List<LocalDate> dates = new ArrayList<>();
        if (!active) {
            return dates;
        }
        int index = generatedOccurrences;
        LocalDate date = frequency.occurrence(startDate, index);
        while (!date.isAfter(today) && (endDate == null || !date.isAfter(endDate))) {
            dates.add(date);
            index++;
            date = frequency.occurrence(startDate, index);
        }
        return dates;
    }

    public RecurringTransaction withGeneratedOccurrences(int newGeneratedOccurrences) {
        return new RecurringTransaction(
                id,
                userId,
                accountId,
                categoryId,
                clientId,
                description,
                amount,
                type,
                frequency,
                startDate,
                endDate,
                newGeneratedOccurrences,
                active,
                createdAt);
    }

    /**
     * Conta, frequência, data inicial e tipo não mudam depois de criada — alterá-los mudaria o
     * significado das ocorrências já lançadas. Ao reativar uma recorrência pausada, as ocorrências
     * que caíram durante a pausa são puladas (pausar significa "não lançar", não "adiar").
     */
    public RecurringTransaction withDetails(
            Long newCategoryId,
            Long newClientId,
            String newDescription,
            BigDecimal newAmount,
            LocalDate newEndDate,
            boolean newActive,
            LocalDate today) {
        int occurrences = generatedOccurrences;
        if (!active && newActive) {
            while (frequency.occurrence(startDate, occurrences).isBefore(today)) {
                occurrences++;
            }
        }
        return new RecurringTransaction(
                id,
                userId,
                accountId,
                newCategoryId,
                newClientId,
                newDescription,
                newAmount,
                type,
                frequency,
                startDate,
                newEndDate,
                occurrences,
                newActive,
                createdAt);
    }
}
