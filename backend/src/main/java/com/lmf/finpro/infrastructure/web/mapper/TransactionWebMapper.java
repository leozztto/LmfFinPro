package com.lmf.finpro.infrastructure.web.mapper;

import com.lmf.finpro.application.support.RecordAuthor;
import com.lmf.finpro.domain.model.Tag;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.infrastructure.web.dto.tag.TagSummaryResponse;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TransactionWebMapper {

    /** Sem contagem de anexos (telas que não mostram o indicador de anexo). */
    public TransactionResponse toResponse(Transaction transaction, List<Tag> tags) {
        return toResponse(transaction, 0, tags);
    }

    public TransactionResponse toResponse(
            Transaction transaction, long attachmentCount, List<Tag> tags) {
        return toResponse(transaction, attachmentCount, tags, null);
    }

    /** {@code author} é quem criou o lançamento; nulo quando não se sabe (criado pelo sistema). */
    /** {@code linkedAccountName}: nome da conta quando ela está em outro espaço; nulo no resto. */
    public TransactionResponse toResponse(
            Transaction transaction,
            long attachmentCount,
            List<Tag> tags,
            RecordAuthor author,
            String linkedAccountName) {
        TransactionResponse base = toResponse(transaction, attachmentCount, tags, author);
        return new TransactionResponse(
                base.id(),
                base.accountId(),
                base.categoryId(),
                base.clientId(),
                base.description(),
                base.amount(),
                base.transactionDate(),
                base.type(),
                base.origin(),
                base.createdAt(),
                base.transferId(),
                base.importBatchId(),
                base.recurringTransactionId(),
                base.status(),
                base.originalCurrency(),
                base.originalAmount(),
                base.baseAmount(),
                base.attachmentCount(),
                base.tags(),
                base.transactionTime(),
                base.createdByUserId(),
                base.createdByName(),
                linkedAccountName);
    }

    public TransactionResponse toResponse(
            Transaction transaction, long attachmentCount, List<Tag> tags, RecordAuthor author) {
        return new TransactionResponse(
                transaction.id(),
                transaction.accountId(),
                transaction.categoryId(),
                transaction.clientId(),
                transaction.description(),
                transaction.amount(),
                transaction.transactionDate(),
                transaction.type(),
                transaction.origin(),
                transaction.createdAt(),
                transaction.transferId(),
                transaction.importBatchId(),
                transaction.recurringTransactionId(),
                transaction.status(),
                transaction.originalCurrency(),
                transaction.originalAmount(),
                transaction.baseAmount(),
                attachmentCount,
                TagSummaryResponse.of(tags),
                transaction.transactionTime(),
                author == null ? null : author.userId(),
                author == null ? null : author.name(),
                null);
    }
}
