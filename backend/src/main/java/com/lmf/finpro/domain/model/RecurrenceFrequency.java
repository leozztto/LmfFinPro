package com.lmf.finpro.domain.model;

import java.time.LocalDate;

public enum RecurrenceFrequency {
    WEEKLY,
    MONTHLY,
    YEARLY;

    /**
     * Data da ocorrência de índice {@code index} (0 = a própria data inicial). Sempre calculada a
     * partir da data inicial, nunca da ocorrência anterior: assim uma recorrência mensal no dia 31
     * cai em 28/02 e volta para 31/03, em vez de ficar presa no dia 28.
     */
    public LocalDate occurrence(LocalDate startDate, int index) {
        return switch (this) {
            case WEEKLY -> startDate.plusWeeks(index);
            case MONTHLY -> startDate.plusMonths(index);
            case YEARLY -> startDate.plusYears(index);
        };
    }
}
