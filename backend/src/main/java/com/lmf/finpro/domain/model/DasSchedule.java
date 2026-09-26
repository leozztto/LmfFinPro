package com.lmf.finpro.domain.model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

/**
 * Vencimento do DAS (MEI e Simples Nacional): dia 20 do mês seguinte à competência. Não considera
 * feriados nem fins de semana — o lembrete é sempre para o dia 20.
 */
public final class DasSchedule {

    public static final int DUE_DAY = 20;

    private DasSchedule() {}

    public static boolean appliesTo(TaxRegime regime) {
        return regime == TaxRegime.MEI || regime == TaxRegime.SIMPLES_NACIONAL;
    }

    public static LocalDate dueDateFor(YearMonth competence) {
        return competence.plusMonths(1).atDay(DUE_DAY);
    }

    /**
     * Competência cujo DAS vence entre {@code today} e {@code today + daysBefore}, se houver. Como
     * o vencimento é sempre no dia 20, só o DAS com vencimento neste mês ou no próximo pode cair na
     * janela.
     */
    public static Optional<YearMonth> competenceDueWithin(LocalDate today, int daysBefore) {
        LocalDate windowEnd = today.plusDays(daysBefore);
        for (YearMonth dueMonth :
                new YearMonth[] {YearMonth.from(today), YearMonth.from(today).plusMonths(1)}) {
            LocalDate dueDate = dueMonth.atDay(DUE_DAY);
            if (!dueDate.isBefore(today) && !dueDate.isAfter(windowEnd)) {
                return Optional.of(dueMonth.minusMonths(1));
            }
        }
        return Optional.empty();
    }
}
