package com.lmf.finpro.infrastructure.web.dto.dashboard;

import java.math.BigDecimal;

public record BreakdownPointResponse(
    Long entityId,
    BigDecimal value
) {
}
