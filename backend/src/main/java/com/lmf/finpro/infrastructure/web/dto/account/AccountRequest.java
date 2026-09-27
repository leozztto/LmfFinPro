package com.lmf.finpro.infrastructure.web.dto.account;

import com.lmf.finpro.domain.model.AccountScope;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * @param scope pessoal (PF) ou da empresa (PJ); opcional — na criação vira pessoal, na edição
 *     mantém o atual
 * @param currency moeda da conta; opcional — na criação vira real, na edição mantém a atual
 */
public record AccountRequest(
        @NotBlank(message = "nome é obrigatório") String name,
        @NotNull(message = "tipo é obrigatório") AccountType type,
        @NotNull(message = "saldo inicial é obrigatório") BigDecimal initialBalance,
        AccountScope scope,
        Currency currency) {

    public AccountRequest(
            String name, AccountType type, BigDecimal initialBalance, AccountScope scope) {
        this(name, type, initialBalance, scope, null);
    }

    public AccountRequest(String name, AccountType type, BigDecimal initialBalance) {
        this(name, type, initialBalance, null);
    }
}
