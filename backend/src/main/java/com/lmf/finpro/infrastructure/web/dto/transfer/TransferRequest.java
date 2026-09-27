package com.lmf.finpro.infrastructure.web.dto.transfer;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @param amount valor que sai da conta de origem, na moeda dela
 * @param receivedAmount valor que entra na conta de destino, na moeda dela — obrigatório só quando
 *     as moedas das contas são diferentes
 */
public record TransferRequest(
        @NotNull(message = "conta de origem é obrigatória") Long fromAccountId,
        @NotNull(message = "conta de destino é obrigatória") Long toAccountId,
        @NotNull(message = "valor é obrigatório")
                @DecimalMin(value = "0.01", message = "valor deve ser maior que zero")
                BigDecimal amount,
        @NotNull(message = "data é obrigatória") LocalDate transferDate,
        String description,
        @DecimalMin(value = "0.01", message = "valor recebido deve ser maior que zero")
                BigDecimal receivedAmount) {

    @JsonCreator
    public TransferRequest {}

    /** Entre contas da mesma moeda. */
    public TransferRequest(
            Long fromAccountId,
            Long toAccountId,
            BigDecimal amount,
            LocalDate transferDate,
            String description) {
        this(fromAccountId, toAccountId, amount, transferDate, description, null);
    }
}
