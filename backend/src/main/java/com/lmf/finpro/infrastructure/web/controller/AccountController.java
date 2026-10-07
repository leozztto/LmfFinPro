package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.account.AccountApplicationService;
import com.lmf.finpro.application.exchangerate.ExchangeRateApplicationService;
import com.lmf.finpro.application.support.RecordAuthor;
import com.lmf.finpro.application.support.RecordAuthorshipApplicationService;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.HouseholdRole;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.account.AccountRequest;
import com.lmf.finpro.infrastructure.web.dto.account.AccountResponse;
import com.lmf.finpro.infrastructure.web.mapper.AccountWebMapper;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
    private final RecordAuthorshipApplicationService recordAuthorshipApplicationService;

    @GetMapping
    public List<AccountResponse> list(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        List<Account> accounts = accountApplicationService.list(currentUser.householdId());
        Map<Long, RecordAuthor> owners =
                recordAuthorshipApplicationService.ownersOfAccounts(
                        accounts.stream().map(Account::id).toList());
        return accounts.stream().map(account -> toResponse(account, currentUser, owners)).toList();
    }

    @GetMapping("/{id}")
    public AccountResponse getById(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        return toResponse(
                accountApplicationService.getById(currentUser.householdId(), id), currentUser);
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody AccountRequest request) {
        Account created =
                accountApplicationService.create(
                        currentUser.householdId(),
                        currentUser.userId(),
                        request.name(),
                        request.type(),
                        request.initialBalance(),
                        request.scope(),
                        request.currency());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created, currentUser));
    }

    @PutMapping("/{id}")
    public AccountResponse update(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long id,
            @Valid @RequestBody AccountRequest request) {
        Account updated =
                accountApplicationService.update(
                        currentUser.householdId(),
                        id,
                        request.name(),
                        request.type(),
                        request.scope(),
                        request.currency());
        return toResponse(updated, currentUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        accountApplicationService.delete(currentUser.householdId(), id);
        return ResponseEntity.noContent().build();
    }

    private AccountResponse toResponse(Account account, AuthenticatedUser currentUser) {
        return toResponse(
                account,
                currentUser,
                recordAuthorshipApplicationService.ownersOfAccounts(List.of(account.id())));
    }

    private AccountResponse toResponse(
            Account account, AuthenticatedUser currentUser, Map<Long, RecordAuthor> owners) {
        RecordAuthor owner = owners.get(account.id());
        Long ownerUserId = owner == null ? null : owner.userId();
        // Dono da conta, ou o dono do grupo quando ninguém sabe quem é (conta compartilhada antes
        // de a conta ter dono). Só vale para conta que está num grupo; quem decide é o servidor.
        boolean canUnshare =
                currentUser.userId().equals(ownerUserId)
                        || (ownerUserId == null
                                && currentUser.householdRole() == HouseholdRole.OWNER);
        BigDecimal currentBalance = accountApplicationService.calculateCurrentBalance(account);
        return mapper.toResponse(
                account,
                currentBalance,
                exchangeRateApplicationService.toBrlTodayOrNull(account.currency(), currentBalance),
                accountApplicationService.hasLinkedRecords(account.id()),
                ownerUserId,
                owner == null ? null : owner.name(),
                canUnshare);
    }
}
