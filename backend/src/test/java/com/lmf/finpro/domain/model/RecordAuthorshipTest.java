package com.lmf.finpro.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RecordAuthorshipTest {

    @Test
    void theAuthorCanDelete() {
        assertThat(RecordAuthorship.canDelete(20L, 20L)).isTrue();
    }

    @Test
    void anotherUserCannotDelete() {
        assertThat(RecordAuthorship.canDelete(30L, 20L)).isFalse();
    }

    @Test
    void whenTheAuthorIsUnknownAnyMemberCanDelete() {
        // Criado pelo sistema (recorrência, aporte automático) ou antes de a autoria existir.
        assertThat(RecordAuthorship.canDelete(null, 20L)).isTrue();
    }
}
