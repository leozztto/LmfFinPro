package com.lmf.finpro.domain.model;

import java.math.BigDecimal;

/** Total por categoria ou cliente num mês — {@code entityId} nulo agrupa "sem categoria"/"sem cliente". */
public record BreakdownPoint(Long entityId, BigDecimal value) {
}
