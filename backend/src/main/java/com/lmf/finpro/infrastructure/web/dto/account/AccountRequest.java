package com.lmf.finpro.infrastructure.web.dto.account;

import com.lmf.finpro.domain.model.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AccountRequest(
        @NotBlank(message = "nome é obrigatório") String name,
        @NotNull(message = "tipo é obrigatório") AccountType type,
        @NotNull(message = "saldo inicial é obrigatório") BigDecimal initialBalance) {}
