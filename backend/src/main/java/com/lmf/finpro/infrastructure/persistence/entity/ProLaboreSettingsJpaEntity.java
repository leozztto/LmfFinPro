package com.lmf.finpro.infrastructure.persistence.entity;

import com.lmf.finpro.domain.model.ProLaboreCalculationBase;
import com.lmf.finpro.domain.model.ProLaboreTaxMode;
import com.lmf.finpro.domain.model.ProLaboreWithholdingMode;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(name = "pro_labore_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProLaboreSettingsJpaEntity {

    @Id
    @Column(name = "household_id")
    private Long householdId;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_base", nullable = false, length = 20)
    private ProLaboreCalculationBase calculationBase;

    @Column(name = "cash_cushion_months", nullable = false)
    private int cashCushionMonths;

    @Column(name = "reserve_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal reserveRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_mode", nullable = false, length = 20)
    private ProLaboreTaxMode taxMode;

    @Column(name = "manual_tax_rate", precision = 6, scale = 4)
    private BigDecimal manualTaxRate;

    @Column(name = "fixed_amount", precision = 14, scale = 2)
    private BigDecimal fixedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "withholding_mode", nullable = false, length = 20)
    private ProLaboreWithholdingMode withholdingMode;

    @Column(name = "employer_inss_rate", precision = 6, scale = 4)
    private BigDecimal employerInssRate;
}
