package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.attachment.TransactionAttachmentApplicationService;
import com.lmf.finpro.application.tag.TagApplicationService;
import com.lmf.finpro.application.transaction.TransactionApplicationService;
import com.lmf.finpro.domain.model.PageResult;
import com.lmf.finpro.domain.model.Tag;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.common.PageResponse;
import com.lmf.finpro.infrastructure.web.dto.tag.TagNamesRequest;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionListRequest;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionRequest;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionResponse;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionStatusRequest;
import com.lmf.finpro.infrastructure.web.mapper.TransactionWebMapper;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionApplicationService transactionApplicationService;
    private final TransactionWebMapper mapper;
    private final TransactionAttachmentApplicationService transactionAttachmentApplicationService;
    private final TagApplicationService tagApplicationService;

    /**
     * Cada transação vem com a quantidade de anexos e as tags — uma consulta agrupada de cada, para
     * a lista toda.
     */
    @GetMapping
    public PageResponse<TransactionResponse> list(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            TransactionListRequest request) {
        PageResult<Transaction> page =
                transactionApplicationService.list(
                        currentUser.userId(), request.toFilters(), request.page(), request.size());
        return PageResponse.of(toResponses(currentUser.userId(), page.content()), page);
    }

    @GetMapping("/{id}")
    public TransactionResponse getById(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        Transaction transaction = transactionApplicationService.getById(currentUser.userId(), id);
        return toResponses(currentUser.userId(), List.of(transaction)).get(0);
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody TransactionRequest request) {
        Transaction created =
                transactionApplicationService.create(
                        currentUser.userId(),
                        request.accountId(),
                        request.categoryId(),
                        request.clientId(),
                        request.description(),
                        request.amount(),
                        request.transactionDate(),
                        request.type(),
                        request.status(),
                        request.tagNames(),
                        request.originalCurrency(),
                        request.originalAmount());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponses(currentUser.userId(), List.of(created)).get(0));
    }

    @PutMapping("/{id}")
    public TransactionResponse update(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request) {
        Transaction updated =
                transactionApplicationService.update(
                        currentUser.userId(),
                        id,
                        request.categoryId(),
                        request.clientId(),
                        request.description(),
                        request.amount(),
                        request.transactionDate(),
                        request.type(),
                        request.status(),
                        request.tagNames(),
                        request.originalCurrency(),
                        request.originalAmount());
        return toResponses(currentUser.userId(), List.of(updated)).get(0);
    }

    @PatchMapping("/{id}/status")
    public TransactionResponse updateStatus(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long id,
            @Valid @RequestBody TransactionStatusRequest request) {
        Transaction updated =
                transactionApplicationService.updateStatus(
                        currentUser.userId(), id, request.status());
        return toResponses(currentUser.userId(), List.of(updated)).get(0);
    }

    /** Troca só as tags (inclusive de transação paga, importada ou de transferência). */
    @PutMapping("/{id}/tags")
    public TransactionResponse updateTags(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long id,
            @Valid @RequestBody TagNamesRequest request) {
        Transaction transaction =
                transactionApplicationService.updateTags(
                        currentUser.userId(), id, request.tagNames());
        return toResponses(currentUser.userId(), List.of(transaction)).get(0);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        transactionApplicationService.delete(currentUser.userId(), id);
        return ResponseEntity.noContent().build();
    }

    private List<TransactionResponse> toResponses(Long userId, List<Transaction> transactions) {
        List<Long> ids = transactions.stream().map(Transaction::id).toList();
        Map<Long, Long> attachmentCounts =
                transactionAttachmentApplicationService.countByTransactionIds(ids);
        Map<Long, List<Tag>> tags = tagApplicationService.tagsByTransactionIds(userId, ids);
        return transactions.stream()
                .map(
                        transaction ->
                                mapper.toResponse(
                                        transaction,
                                        attachmentCounts.getOrDefault(transaction.id(), 0L),
                                        tags.getOrDefault(transaction.id(), List.of())))
                .toList();
    }
}
