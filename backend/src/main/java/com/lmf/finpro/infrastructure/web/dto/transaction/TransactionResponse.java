package com.lmf.finpro.infrastructure.web.dto.transaction;

import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.TransactionOrigin;
import com.lmf.finpro.domain.model.TransactionStatus;
import com.lmf.finpro.infrastructure.web.dto.tag.TagSummaryResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TransactionResponse(
        Long id,
        Long accountId,
        Long categoryId,
        Long clientId,
        String description,
        BigDecimal amount,
        LocalDate transactionDate,
        CategoryType type,
        TransactionOrigin origin,
        LocalDateTime createdAt,
        Long transferId,
        Long importBatchId,
        Long recurringTransactionId,
        TransactionStatus status,
        long attachmentCount,
        List<TagSummaryResponse> tags) {}
