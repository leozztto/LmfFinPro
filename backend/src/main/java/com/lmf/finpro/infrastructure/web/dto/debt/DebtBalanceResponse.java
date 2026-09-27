package com.lmf.finpro.infrastructure.web.dto.debt;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DebtBalanceResponse(
        Long id, Long debtId, LocalDate balanceDate, BigDecimal balance) {}
