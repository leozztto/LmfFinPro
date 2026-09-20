package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.YearMonth;

public record BalancePoint(YearMonth month, BigDecimal balance) {
}
