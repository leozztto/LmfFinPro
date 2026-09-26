package com.lmf.finpro.infrastructure.web.dto.savingsgoal;

import java.math.BigDecimal;

/**
 * @param incomeRate fração sugerida para a caixinha do imposto (0.06 = 6%)
 */
public record SuggestedTaxRateResponse(BigDecimal incomeRate) {}
