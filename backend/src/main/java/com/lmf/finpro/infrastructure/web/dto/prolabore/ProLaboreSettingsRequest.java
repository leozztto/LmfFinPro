package com.lmf.finpro.infrastructure.web.dto.prolabore;

import com.lmf.finpro.domain.model.ProLaboreCalculationBase;
import com.lmf.finpro.domain.model.ProLaboreTaxMode;
import com.lmf.finpro.domain.model.ProLaboreWithholdingMode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * @param reserveRate fração da receita retida na empresa (0.10 = 10%)
 * @param manualTaxRate alíquota do modo manual (0.06 = 6%); obrigatória só no modo manual
 * @param fixedAmount pró-labore fixo mensal; vazio = sem valor fixo
 */
public record ProLaboreSettingsRequest(
        @NotNull(message = "base do cálculo é obrigatória")
                ProLaboreCalculationBase calculationBase,
        @NotNull(message = "meses de colchão de caixa é obrigatório")
                @Min(value = 0, message = "colchão de caixa deve ser de 0 a 12 meses")
                @Max(value = 12, message = "colchão de caixa deve ser de 0 a 12 meses")
                Integer cashCushionMonths,
        @NotNull(message = "percentual de reserva é obrigatório")
                @DecimalMin(value = "0.0", message = "reserva não pode ser negativa")
                @DecimalMax(value = "1.0", message = "reserva deve ser no máximo 100%")
                BigDecimal reserveRate,
        @NotNull(message = "modo do imposto é obrigatório") ProLaboreTaxMode taxMode,
        @DecimalMin(value = "0.0", message = "alíquota não pode ser negativa")
                @DecimalMax(value = "1.0", message = "alíquota deve ser no máximo 100%")
                BigDecimal manualTaxRate,
        @DecimalMin(value = "0.0", message = "pró-labore fixo não pode ser negativo")
                BigDecimal fixedAmount,
        ProLaboreWithholdingMode withholdingMode,
        @DecimalMin(value = "0.0", message = "INSS patronal não pode ser negativo")
                @DecimalMax(value = "1.0", message = "INSS patronal deve ser no máximo 100%")
                BigDecimal employerInssRate) {

    /** Sem os campos de encargos: retenção automática pelo regime e INSS patronal automático. */
    public ProLaboreSettingsRequest(
            ProLaboreCalculationBase calculationBase,
            Integer cashCushionMonths,
            BigDecimal reserveRate,
            ProLaboreTaxMode taxMode,
            BigDecimal manualTaxRate,
            BigDecimal fixedAmount) {
        this(
                calculationBase,
                cashCushionMonths,
                reserveRate,
                taxMode,
                manualTaxRate,
                fixedAmount,
                ProLaboreWithholdingMode.AUTOMATIC,
                null);
    }
}
