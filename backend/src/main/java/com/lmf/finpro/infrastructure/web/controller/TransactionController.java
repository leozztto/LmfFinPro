package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.transaction.TransactionApplicationService;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionRequest;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionResponse;
import com.lmf.finpro.infrastructure.web.mapper.TransactionWebMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionApplicationService transactionApplicationService;
    private final TransactionWebMapper mapper;

    @GetMapping
    public List<TransactionResponse> list(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return transactionApplicationService.list(currentUser.userId()).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public TransactionResponse getById(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        return mapper.toResponse(transactionApplicationService.getById(currentUser.userId(), id));
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> create(
        @AuthenticationPrincipal AuthenticatedUser currentUser, @Valid @RequestBody TransactionRequest request
    ) {
        Transaction created = transactionApplicationService.create(
            currentUser.userId(), request.accountId(), request.categoryId(), request.clientId(),
            request.description(), request.amount(), request.transactionDate(), request.type()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(created));
    }

    @PutMapping("/{id}")
    public TransactionResponse update(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @PathVariable Long id,
        @Valid @RequestBody TransactionRequest request
    ) {
        Transaction updated = transactionApplicationService.update(
            currentUser.userId(), id, request.categoryId(), request.clientId(),
            request.description(), request.amount(), request.transactionDate(), request.type()
        );
        return mapper.toResponse(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        transactionApplicationService.delete(currentUser.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
