package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.recurringbudget.RecurringBudgetApplicationService;
import com.lmf.finpro.domain.model.RecurringBudget;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.recurringbudget.RecurringBudgetRequest;
import com.lmf.finpro.infrastructure.web.dto.recurringbudget.RecurringBudgetResponse;
import com.lmf.finpro.infrastructure.web.dto.recurringbudget.RecurringBudgetUpdateRequest;
import com.lmf.finpro.infrastructure.web.mapper.RecurringBudgetWebMapper;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recurring-budgets")
@RequiredArgsConstructor
public class RecurringBudgetController {

    private final RecurringBudgetApplicationService recurringBudgetApplicationService;
    private final RecurringBudgetWebMapper mapper;

    @GetMapping
    public List<RecurringBudgetResponse> list(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return recurringBudgetApplicationService.list(currentUser.userId()).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @PostMapping
    public ResponseEntity<RecurringBudgetResponse> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody RecurringBudgetRequest request) {
        RecurringBudget created =
                recurringBudgetApplicationService.create(
                        currentUser.userId(),
                        request.categoryId(),
                        request.limitValue(),
                        request.startMonth(),
                        request.endMonth());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(created));
    }

    @PutMapping("/{id}")
    public RecurringBudgetResponse update(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long id,
            @Valid @RequestBody RecurringBudgetUpdateRequest request) {
        RecurringBudget updated =
                recurringBudgetApplicationService.update(
                        currentUser.userId(),
                        id,
                        request.limitValue(),
                        request.endMonth(),
                        request.active());
        return mapper.toResponse(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        recurringBudgetApplicationService.delete(currentUser.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
