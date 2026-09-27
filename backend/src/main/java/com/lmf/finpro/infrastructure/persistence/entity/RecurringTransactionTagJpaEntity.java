package com.lmf.finpro.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import lombok.*;

/** Vínculo recorrência ↔ tag (tabela {@code recurring_transaction_tags}). */
@Entity
@Table(name = "recurring_transaction_tags")
@IdClass(RecurringTransactionTagJpaEntity.Key.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecurringTransactionTagJpaEntity {

    @Id
    @Column(name = "recurring_transaction_id")
    private Long recurringTransactionId;

    @Id
    @Column(name = "tag_id")
    private Long tagId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Key implements Serializable {
        private Long recurringTransactionId;
        private Long tagId;
    }
}
