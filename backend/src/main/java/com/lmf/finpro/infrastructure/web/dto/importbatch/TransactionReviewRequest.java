package com.lmf.finpro.infrastructure.web.dto.importbatch;

/** {@code null} em qualquer um dos campos significa "sem categoria"/"sem cliente". */
public record TransactionReviewRequest(
    Long categoryId,
    Long clientId
) {
}
