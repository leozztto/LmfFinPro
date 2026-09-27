package com.lmf.finpro.infrastructure.web.dto.account;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AccountValuationResponse(
        Long id, Long accountId, LocalDate valuationDate, BigDecimal value) {}
