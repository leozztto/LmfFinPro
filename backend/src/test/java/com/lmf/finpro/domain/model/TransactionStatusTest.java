package com.lmf.finpro.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TransactionStatusTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 25);

    @Test
    void futureDateDefaultsToPending() {
        assertThat(TransactionStatus.defaultFor(TODAY.plusDays(1), TODAY))
                .isEqualTo(TransactionStatus.PENDING);
    }

    @Test
    void todayDefaultsToPaid() {
        assertThat(TransactionStatus.defaultFor(TODAY, TODAY)).isEqualTo(TransactionStatus.PAID);
    }

    @Test
    void pastDateDefaultsToPaid() {
        assertThat(TransactionStatus.defaultFor(TODAY.minusMonths(1), TODAY))
                .isEqualTo(TransactionStatus.PAID);
    }
}
