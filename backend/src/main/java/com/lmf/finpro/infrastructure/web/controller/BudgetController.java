package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.budget.BudgetApplicationService;
import com.lmf.finpro.domain.model.Budget;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.budget.BudgetRequest;
import com.lmf.finpro.infrastructure.web.dto.budget.BudgetResponse;
import com.lmf.finpro.infrastructure.web.mapper.BudgetWebMapper;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetApplicationService budgetApplicationService;
    private final BudgetWebMapper mapper;

    @GetMapping
    public List<BudgetResponse> list(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return budgetApplicationService.list(currentUser.userId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody BudgetRequest request) {
        Budget created =
                budgetApplicationService.create(
                        currentUser.userId(),
                        request.categoryId(),
                        request.referenceMonth(),
                        request.limitValue());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        budgetApplicationService.delete(currentUser.userId(), id);
        return ResponseEntity.noContent().build();
    }

    private BudgetResponse toResponse(Budget budget) {
        return mapper.toResponse(budget, budgetApplicationService.calculateSpent(budget));
    }
}
