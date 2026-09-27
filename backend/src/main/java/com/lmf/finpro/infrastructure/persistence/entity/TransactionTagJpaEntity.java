package com.lmf.finpro.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import lombok.*;

/** Vínculo transação ↔ tag (tabela {@code transaction_tags}). */
@Entity
@Table(name = "transaction_tags")
@IdClass(TransactionTagJpaEntity.Key.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionTagJpaEntity {

    @Id
    @Column(name = "transaction_id")
    private Long transactionId;

    @Id
    @Column(name = "tag_id")
    private Long tagId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Key implements Serializable {
        private Long transactionId;
        private Long tagId;
    }
}
