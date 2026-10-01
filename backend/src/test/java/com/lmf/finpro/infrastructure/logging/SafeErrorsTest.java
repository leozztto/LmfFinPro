package com.lmf.finpro.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SafeErrorsTest {

    @Test
    void describesTypeAndRootCauseWithoutMessages() {
        Exception ex =
                new IllegalStateException(
                        "falha com maria@example.com",
                        new IllegalArgumentException("Key (email)=(maria@example.com)"));

        assertThat(SafeErrors.describe(ex))
                .isEqualTo("IllegalStateException (causa: IllegalArgumentException)")
                .doesNotContain("maria");
    }

    @Test
    void exceptionWithoutCauseIsJustItsType() {
        assertThat(SafeErrors.describe(new IllegalStateException("x")))
                .isEqualTo("IllegalStateException");
    }
}
