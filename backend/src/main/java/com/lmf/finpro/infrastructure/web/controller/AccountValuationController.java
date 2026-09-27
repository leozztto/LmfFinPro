package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.account.AccountValuationApplicationService;
import com.lmf.finpro.domain.model.AccountValuation;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.account.AccountValuationRequest;
import com.lmf.finpro.infrastructure.web.dto.account.AccountValuationResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Valor de mercado das contas de investimento. */
@RestController
@RequestMapping("/api/accounts/{accountId}/valuations")
@RequiredArgsConstructor
public class AccountValuationController {

    private final AccountValuationApplicationService accountValuationApplicationService;

    @GetMapping
    public List<AccountValuationResponse> list(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long accountId) {
        return accountValuationApplicationService.list(currentUser.userId(), accountId).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Informar uma data que já tem valor substitui o anterior. */
    @PostMapping
    public ResponseEntity<AccountValuationResponse> save(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long accountId,
            @Valid @RequestBody AccountValuationRequest request) {
        AccountValuation saved =
                accountValuationApplicationService.save(
                        currentUser.userId(), accountId, request.valuationDate(), request.value());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @DeleteMapping("/{valuationId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long accountId,
            @PathVariable Long valuationId) {
        accountValuationApplicationService.delete(currentUser.userId(), accountId, valuationId);
        return ResponseEntity.noContent().build();
    }

    private AccountValuationResponse toResponse(AccountValuation valuation) {
        return new AccountValuationResponse(
                valuation.id(),
                valuation.accountId(),
                valuation.valuationDate(),
                valuation.value());
    }
}
