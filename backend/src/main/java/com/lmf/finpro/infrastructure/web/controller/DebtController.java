package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.debt.DebtApplicationService;
import com.lmf.finpro.application.debt.DebtApplicationService.DebtSummary;
import com.lmf.finpro.domain.model.DebtBalance;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.debt.DebtBalanceRequest;
import com.lmf.finpro.infrastructure.web.dto.debt.DebtBalanceResponse;
import com.lmf.finpro.infrastructure.web.dto.debt.DebtCreateRequest;
import com.lmf.finpro.infrastructure.web.dto.debt.DebtResponse;
import com.lmf.finpro.infrastructure.web.dto.debt.DebtUpdateRequest;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Dívidas acompanhadas no patrimônio e o histórico do saldo devedor de cada uma. */
@RestController
@RequestMapping("/api/debts")
@RequiredArgsConstructor
public class DebtController {

    private final DebtApplicationService debtApplicationService;

    @GetMapping
    public List<DebtResponse> list(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return debtApplicationService.list(currentUser.userId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    public ResponseEntity<DebtResponse> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody DebtCreateRequest request) {
        DebtSummary created =
                debtApplicationService.create(
                        currentUser.userId(),
                        request.name(),
                        request.type(),
                        request.creditor(),
                        request.balance(),
                        request.balanceDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PutMapping("/{id}")
    public DebtResponse update(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long id,
            @Valid @RequestBody DebtUpdateRequest request) {
        return toResponse(
                debtApplicationService.update(
                        currentUser.userId(),
                        id,
                        request.name(),
                        request.type(),
                        request.creditor()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        debtApplicationService.delete(currentUser.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/balances")
    public List<DebtBalanceResponse> listBalances(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        return debtApplicationService.listBalances(currentUser.userId(), id).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Informar uma data que já tem saldo substitui o anterior. */
    @PostMapping("/{id}/balances")
    public ResponseEntity<DebtBalanceResponse> saveBalance(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long id,
            @Valid @RequestBody DebtBalanceRequest request) {
        DebtBalance saved =
                debtApplicationService.saveBalance(
                        currentUser.userId(), id, request.balanceDate(), request.balance());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @DeleteMapping("/{id}/balances/{balanceId}")
    public ResponseEntity<Void> deleteBalance(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long id,
            @PathVariable Long balanceId) {
        debtApplicationService.deleteBalance(currentUser.userId(), id, balanceId);
        return ResponseEntity.noContent().build();
    }

    private DebtResponse toResponse(DebtSummary summary) {
        return new DebtResponse(
                summary.debt().id(),
                summary.debt().name(),
                summary.debt().type(),
                summary.debt().creditor(),
                summary.currentBalance(),
                summary.lastBalance() == null ? null : summary.lastBalance().balanceDate());
    }

    private DebtBalanceResponse toResponse(DebtBalance balance) {
        return new DebtBalanceResponse(
                balance.id(), balance.debtId(), balance.balanceDate(), balance.balance());
    }
}
