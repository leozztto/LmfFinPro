package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.account.AccountApplicationService;
import com.lmf.finpro.application.exchangerate.ExchangeRateApplicationService;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.account.AccountRequest;
import com.lmf.finpro.infrastructure.web.dto.account.AccountResponse;
import com.lmf.finpro.infrastructure.web.mapper.AccountWebMapper;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountApplicationService accountApplicationService;
    private final ExchangeRateApplicationService exchangeRateApplicationService;
    private final AccountWebMapper mapper;

    @GetMapping
    public List<AccountResponse> list(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return accountApplicationService.list(currentUser.userId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public AccountResponse getById(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        return toResponse(accountApplicationService.getById(currentUser.userId(), id));
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody AccountRequest request) {
        Account created =
                accountApplicationService.create(
                        currentUser.userId(),
                        request.name(),
                        request.type(),
                        request.initialBalance(),
                        request.scope(),
                        request.currency());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PutMapping("/{id}")
    public AccountResponse update(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long id,
            @Valid @RequestBody AccountRequest request) {
        Account updated =
                accountApplicationService.update(
                        currentUser.userId(),
                        id,
                        request.name(),
                        request.type(),
                        request.scope(),
                        request.currency());
        return toResponse(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        accountApplicationService.delete(currentUser.userId(), id);
        return ResponseEntity.noContent().build();
    }

    private AccountResponse toResponse(Account account) {
        BigDecimal currentBalance = accountApplicationService.calculateCurrentBalance(account);
        return mapper.toResponse(
                account,
                currentBalance,
                exchangeRateApplicationService.toBrlTodayOrNull(account.currency(), currentBalance),
                accountApplicationService.hasLinkedRecords(account.id()));
    }
}
