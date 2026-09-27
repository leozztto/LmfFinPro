package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.recurringtransaction.RecurringTransactionApplicationService;
import com.lmf.finpro.application.tag.TagApplicationService;
import com.lmf.finpro.domain.model.RecurringTransaction;
import com.lmf.finpro.domain.model.Tag;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.recurringtransaction.RecurringTransactionRequest;
import com.lmf.finpro.infrastructure.web.dto.recurringtransaction.RecurringTransactionResponse;
import com.lmf.finpro.infrastructure.web.dto.recurringtransaction.RecurringTransactionUpdateRequest;
import com.lmf.finpro.infrastructure.web.mapper.RecurringTransactionWebMapper;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recurring-transactions")
@RequiredArgsConstructor
public class RecurringTransactionController {

    private final RecurringTransactionApplicationService recurringTransactionApplicationService;
    private final RecurringTransactionWebMapper mapper;
    private final TagApplicationService tagApplicationService;

    @GetMapping
    public List<RecurringTransactionResponse> list(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return toResponses(
                currentUser.userId(),
                recurringTransactionApplicationService.list(currentUser.userId()));
    }

    @PostMapping
    public ResponseEntity<RecurringTransactionResponse> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody RecurringTransactionRequest request) {
        RecurringTransaction created =
                recurringTransactionApplicationService.create(
                        currentUser.userId(),
                        request.accountId(),
                        request.categoryId(),
                        request.clientId(),
                        request.description(),
                        request.amount(),
                        request.type(),
                        request.frequency(),
                        request.startDate(),
                        request.endDate(),
                        request.tagNames());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponses(currentUser.userId(), List.of(created)).get(0));
    }

    @PutMapping("/{id}")
    public RecurringTransactionResponse update(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long id,
            @Valid @RequestBody RecurringTransactionUpdateRequest request) {
        RecurringTransaction updated =
                recurringTransactionApplicationService.update(
                        currentUser.userId(),
                        id,
                        request.categoryId(),
                        request.clientId(),
                        request.description(),
                        request.amount(),
                        request.endDate(),
                        request.active(),
                        request.tagNames());
        return toResponses(currentUser.userId(), List.of(updated)).get(0);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        recurringTransactionApplicationService.delete(currentUser.userId(), id);
        return ResponseEntity.noContent().build();
    }

    private List<RecurringTransactionResponse> toResponses(
            Long userId, List<RecurringTransaction> recurrences) {
        Map<Long, List<Tag>> tags =
                tagApplicationService.tagsByRecurringTransactionIds(
                        userId, recurrences.stream().map(RecurringTransaction::id).toList());
        return recurrences.stream()
                .map(
                        recurrence ->
                                mapper.toResponse(
                                        recurrence, tags.getOrDefault(recurrence.id(), List.of())))
                .toList();
    }
}
