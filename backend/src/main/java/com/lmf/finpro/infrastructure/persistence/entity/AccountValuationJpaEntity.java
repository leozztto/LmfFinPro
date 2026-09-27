package com.lmf.finpro.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "account_valuations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountValuationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "valuation_date", nullable = false)
    private LocalDate valuationDate;

    @Column(name = "value", nullable = false, precision = 14, scale = 2)
    private BigDecimal value;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
