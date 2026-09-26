package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.recurringtransaction.RecurringTransactionApplicationService;
import com.lmf.finpro.domain.model.RecurringTransaction;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.recurringtransaction.RecurringTransactionRequest;
import com.lmf.finpro.infrastructure.web.dto.recurringtransaction.RecurringTransactionResponse;
import com.lmf.finpro.infrastructure.web.dto.recurringtransaction.RecurringTransactionUpdateRequest;
import com.lmf.finpro.infrastructure.web.mapper.RecurringTransactionWebMapper;
import jakarta.validation.Valid;
import java.util.List;
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

    @GetMapping
    public List<RecurringTransactionResponse> list(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return recurringTransactionApplicationService.list(currentUser.userId()).stream()
                .map(mapper::toResponse)
                .toList();
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
                        request.endDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(created));
    }

    @PutMapping("/{id}")
    public RecurringTransactionResponse update(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long id,
            @Valid @RequestBody RecurringTransactionUpdateRequest request) {
        return mapper.toResponse(
                recurringTransactionApplicationService.update(
                        currentUser.userId(),
                        id,
                        request.categoryId(),
                        request.clientId(),
                        request.description(),
                        request.amount(),
                        request.endDate(),
                        request.active()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        recurringTransactionApplicationService.delete(currentUser.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
