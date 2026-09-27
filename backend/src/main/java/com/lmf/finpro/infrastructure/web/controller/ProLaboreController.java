package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.prolabore.ProLaboreApplicationService;
import com.lmf.finpro.application.prolabore.ProLaboreSummary;
import com.lmf.finpro.domain.model.PayrollTaxCalculator.PayrollTaxes;
import com.lmf.finpro.domain.model.ProLaboreCalculator;
import com.lmf.finpro.domain.model.ProLaboreSettings;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.prolabore.ProLaboreResponse;
import com.lmf.finpro.infrastructure.web.dto.prolabore.ProLaboreResponse.BusinessExpenseResponse;
import com.lmf.finpro.infrastructure.web.dto.prolabore.ProLaboreResponse.PayrollResponse;
import com.lmf.finpro.infrastructure.web.dto.prolabore.ProLaboreResponse.SettingsResponse;
import com.lmf.finpro.infrastructure.web.dto.prolabore.ProLaboreResponse.WithdrawalResponse;
import com.lmf.finpro.infrastructure.web.dto.prolabore.ProLaboreSettingsRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pro-labore")
@RequiredArgsConstructor
public class ProLaboreController {

    private final ProLaboreApplicationService proLaboreApplicationService;

    @GetMapping
    public ProLaboreResponse summary(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return toResponse(proLaboreApplicationService.summary(currentUser.userId()));
    }

    /** Devolve o resumo já recalculado com a nova configuração. */
    @PutMapping("/settings")
    public ProLaboreResponse updateSettings(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody ProLaboreSettingsRequest request) {
        proLaboreApplicationService.updateSettings(
                currentUser.userId(),
                request.calculationBase(),
                request.cashCushionMonths(),
                request.reserveRate(),
                request.taxMode(),
                request.manualTaxRate(),
                request.fixedAmount(),
                request.withholdingMode(),
                request.employerInssRate());
        return toResponse(proLaboreApplicationService.summary(currentUser.userId()));
    }

    private ProLaboreResponse toResponse(ProLaboreSummary summary) {
        ProLaboreCalculator.Result result = summary.result();
        ProLaboreSettings settings = summary.settings();
        PayrollTaxes payroll = result.payroll();
        return new ProLaboreResponse(
                summary.hasBusinessAccounts(),
                summary.hasPersonalAccounts(),
                new SettingsResponse(
                        settings.calculationBase().name(),
                        settings.cashCushionMonths(),
                        settings.reserveRate(),
                        settings.taxMode().name(),
                        settings.manualTaxRate(),
                        settings.fixedAmount(),
                        settings.withholdingMode().name(),
                        settings.employerInssRate()),
                result.availableToWithdraw(),
                result.suggestedPayment(),
                result.calculatedAmount(),
                result.balanceCap(),
                result.cappedByBalance(),
                result.monthBudget(),
                summary.businessBalance(),
                summary.monthBusinessIncome(),
                summary.monthBusinessExpenses(),
                summary.pendingBusinessExpenses(),
                summary.taxRate(),
                result.taxOnMonthIncome(),
                summary.taxReserveSaved(),
                result.taxReserve(),
                result.reserve(),
                summary.averageMonthlyBusinessExpense(),
                result.cashCushion(),
                result.withholdingApplied(),
                result.employerInssRate(),
                new PayrollResponse(
                        payroll.gross(),
                        payroll.employeeInss(),
                        payroll.irrf(),
                        payroll.employerInss(),
                        payroll.net(),
                        payroll.taxesToCollect()),
                result.fixedRemaining(),
                result.fixedCovered(),
                summary.withdrawnThisMonth(),
                summary.withdrawals().stream()
                        .map(
                                withdrawal ->
                                        new WithdrawalResponse(
                                                withdrawal.transferId(),
                                                withdrawal.date(),
                                                withdrawal.amount(),
                                                withdrawal.fromAccountName(),
                                                withdrawal.toAccountName()))
                        .toList(),
                summary.businessExpenses().stream()
                        .map(
                                expense ->
                                        new BusinessExpenseResponse(
                                                expense.transactionId(),
                                                expense.date(),
                                                expense.description(),
                                                expense.amount(),
                                                expense.paid(),
                                                expense.overdue(),
                                                expense.accountName(),
                                                expense.categoryName()))
                        .toList(),
                summary.suggestedFromAccountId(),
                summary.suggestedToAccountId());
    }
}
