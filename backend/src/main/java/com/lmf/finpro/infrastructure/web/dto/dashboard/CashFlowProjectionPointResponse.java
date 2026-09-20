package com.lmf.finpro.infrastructure.web.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.YearMonth;

public record CashFlowProjectionPointResponse(
        @JsonFormat(pattern = "yyyy-MM") YearMonth month, BigDecimal balance, boolean projected) {}
