package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.savingsgoal.SavingsGoalApplicationService;
import com.lmf.finpro.application.savingsgoal.SavingsGoalCommand;
import com.lmf.finpro.application.savingsgoal.SavingsGoalSummary;
import com.lmf.finpro.domain.model.GoalContribution;
import com.lmf.finpro.domain.model.SavingsGoal;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.savingsgoal.GoalContributionRequest;
import com.lmf.finpro.infrastructure.web.dto.savingsgoal.GoalContributionResponse;
import com.lmf.finpro.infrastructure.web.dto.savingsgoal.SavingsGoalRequest;
import com.lmf.finpro.infrastructure.web.dto.savingsgoal.SavingsGoalResponse;
import com.lmf.finpro.infrastructure.web.dto.savingsgoal.SuggestedTaxRateResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/savings-goals")
@RequiredArgsConstructor
public class SavingsGoalController {

    private final SavingsGoalApplicationService savingsGoalApplicationService;

    @GetMapping
    public List<SavingsGoalResponse> list(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return savingsGoalApplicationService.list(currentUser.userId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/suggested-tax-rate")
    public SuggestedTaxRateResponse suggestedTaxRate(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return new SuggestedTaxRateResponse(
                savingsGoalApplicationService.suggestedTaxRate(currentUser.userId()));
    }

    @PostMapping
    public ResponseEntity<SavingsGoalResponse> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody SavingsGoalRequest request) {
        SavingsGoalSummary created =
                savingsGoalApplicationService.create(currentUser.userId(), toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PutMapping("/{id}")
    public SavingsGoalResponse update(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long id,
            @Valid @RequestBody SavingsGoalRequest request) {
        return toResponse(
                savingsGoalApplicationService.update(currentUser.userId(), id, toCommand(request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        savingsGoalApplicationService.delete(currentUser.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/contributions")
    public List<GoalContributionResponse> listContributions(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        return savingsGoalApplicationService.listContributions(currentUser.userId(), id).stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping("/{id}/contributions")
    public ResponseEntity<GoalContributionResponse> addContribution(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long id,
            @Valid @RequestBody GoalContributionRequest request) {
        GoalContribution created =
                savingsGoalApplicationService.addContribution(
                        currentUser.userId(),
                        id,
                        request.type(),
                        request.amount(),
                        request.contributionDate(),
                        request.note());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    /** "Separar com 1 clique": lança o valor sugerido pelo percentual da meta como aporte. */
    @PostMapping("/{id}/contributions/suggested")
    public ResponseEntity<GoalContributionResponse> applySuggestion(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        GoalContribution created =
                savingsGoalApplicationService.applySuggestion(currentUser.userId(), id);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @DeleteMapping("/{id}/contributions/{contributionId}")
    public ResponseEntity<Void> deleteContribution(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long id,
            @PathVariable Long contributionId) {
        savingsGoalApplicationService.deleteContribution(currentUser.userId(), id, contributionId);
        return ResponseEntity.noContent().build();
    }

    private SavingsGoalCommand toCommand(SavingsGoalRequest request) {
        return new SavingsGoalCommand(
                request.name(),
                request.type(),
                request.targetAmount(),
                request.deadline(),
                request.incomeRate());
    }

    private SavingsGoalResponse toResponse(SavingsGoalSummary summary) {
        SavingsGoal goal = summary.goal();
        return new SavingsGoalResponse(
                goal.id(),
                goal.name(),
                goal.type(),
                goal.targetAmount(),
                goal.deadline(),
                goal.incomeRate(),
                summary.savedAmount(),
                summary.remainingAmount(),
                summary.monthlyNeeded(),
                summary.monthPaidIncome(),
                summary.suggestedContribution());
    }

    private GoalContributionResponse toResponse(GoalContribution contribution) {
        return new GoalContributionResponse(
                contribution.id(),
                contribution.goalId(),
                contribution.type(),
                contribution.amount(),
                contribution.contributionDate(),
                contribution.note());
    }
}
