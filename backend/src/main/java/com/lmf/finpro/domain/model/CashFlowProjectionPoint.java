package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.YearMonth;

public record CashFlowProjectionPoint(YearMonth month, BigDecimal balance, boolean projected) {}
