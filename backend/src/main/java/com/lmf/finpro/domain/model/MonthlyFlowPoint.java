package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyFlowPoint(YearMonth month, BigDecimal income, BigDecimal expense) {
}
