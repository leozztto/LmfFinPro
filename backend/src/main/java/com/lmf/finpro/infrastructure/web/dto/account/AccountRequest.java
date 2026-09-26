package com.lmf.finpro.infrastructure.web.dto.account;

import com.lmf.finpro.domain.model.AccountScope;
import com.lmf.finpro.domain.model.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * @param scope pessoal (PF) ou da empresa (PJ); opcional — na criação vira pessoal, na edição
 *     mantém o atual
 */
public record AccountRequest(
        @NotBlank(message = "nome é obrigatório") String name,
        @NotNull(message = "tipo é obrigatório") AccountType type,
        @NotNull(message = "saldo inicial é obrigatório") BigDecimal initialBalance,
        AccountScope scope) {

    public AccountRequest(String name, AccountType type, BigDecimal initialBalance) {
        this(name, type, initialBalance, null);
    }
}
