package com.lmf.finpro.infrastructure.persistence.entity;

import com.lmf.finpro.domain.model.TaxRegime;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tax_estimates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxEstimateJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;

    @Column(name = "reference_month", nullable = false)
    private LocalDate referenceMonth;

    @Enumerated(EnumType.STRING)
    @Column(name = "regime", nullable = false, length = 30)
    private TaxRegime regime;

    @Column(name = "gross_revenue", nullable = false, precision = 14, scale = 2)
    private BigDecimal grossRevenue;

    @Column(name = "applied_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal appliedRate;

    @Column(name = "estimated_value", nullable = false, precision = 14, scale = 2)
    private BigDecimal estimatedValue;
}
