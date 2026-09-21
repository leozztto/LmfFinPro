package com.lmf.finpro.infrastructure.web.dto.account;

import com.lmf.finpro.domain.model.AccountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(
        Long id,
        String name,
        AccountType type,
        BigDecimal initialBalance,
        BigDecimal currentBalance,
        LocalDateTime createdAt) {}
