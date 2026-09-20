package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.account.AccountApplicationService;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.account.AccountRequest;
import com.lmf.finpro.infrastructure.web.dto.account.AccountResponse;
import com.lmf.finpro.infrastructure.web.mapper.AccountWebMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountApplicationService accountApplicationService;
    private final AccountWebMapper mapper;

    @GetMapping
    public List<AccountResponse> list(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return accountApplicationService.list(currentUser.userId()).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public AccountResponse getById(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        return toResponse(accountApplicationService.getById(currentUser.userId(), id));
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(
        @AuthenticationPrincipal AuthenticatedUser currentUser, @Valid @RequestBody AccountRequest request
    ) {
        Account created = accountApplicationService.create(
            currentUser.userId(), request.name(), request.type(), request.initialBalance()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PutMapping("/{id}")
    public AccountResponse update(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @PathVariable Long id,
        @Valid @RequestBody AccountRequest request
    ) {
        Account updated = accountApplicationService.update(currentUser.userId(), id, request.name(), request.type());
        return toResponse(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        accountApplicationService.delete(currentUser.userId(), id);
        return ResponseEntity.noContent().build();
    }

    private AccountResponse toResponse(Account account) {
        return mapper.toResponse(account, accountApplicationService.calculateCurrentBalance(account));
    }
}
