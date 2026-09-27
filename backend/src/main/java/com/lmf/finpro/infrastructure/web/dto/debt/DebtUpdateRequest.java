package com.lmf.finpro.infrastructure.web.dto.debt;

import com.lmf.finpro.domain.model.DebtType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** O saldo devedor muda pelos saldos informados, não pela edição. */
public record DebtUpdateRequest(
        @NotBlank(message = "nome é obrigatório")
                @Size(max = 100, message = "nome deve ter no máximo 100 caracteres")
                String name,
        @NotNull(message = "tipo é obrigatório") DebtType type,
        @Size(max = 100, message = "credor deve ter no máximo 100 caracteres") String creditor) {}
