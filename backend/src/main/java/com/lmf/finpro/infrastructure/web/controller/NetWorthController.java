package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.networth.NetWorthApplicationService;
import com.lmf.finpro.domain.model.NetWorthCalculator;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.networth.NetWorthResponse;
import com.lmf.finpro.infrastructure.web.dto.networth.NetWorthResponse.AccountRow;
import com.lmf.finpro.infrastructure.web.dto.networth.NetWorthResponse.DebtRow;
import com.lmf.finpro.infrastructure.web.dto.networth.NetWorthResponse.InvestmentRow;
import com.lmf.finpro.infrastructure.web.dto.networth.NetWorthResponse.PointRow;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Patrimônio líquido: contas + investimentos − dívidas, hoje e mês a mês. */
@RestController
@RequestMapping("/api/net-worth")
@RequiredArgsConstructor
public class NetWorthController {

    private final NetWorthApplicationService netWorthApplicationService;

    @GetMapping
    public NetWorthResponse netWorth(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "12") int months) {
        NetWorthCalculator.Report report =
                netWorthApplicationService.summary(currentUser.userId(), months);
        return new NetWorthResponse(
                toRow(report.current()),
                report.changeFromPreviousMonth(),
                report.investmentGain(),
                report.history().stream().map(this::toRow).toList(),
                report.accounts().stream()
                        .map(
                                row ->
                                        new AccountRow(
                                                row.account().id(),
                                                row.account().name(),
                                                row.account().scope(),
                                                row.balance(),
                                                row.account().currency(),
                                                row.balanceInBrl()))
                        .toList(),
                report.investments().stream()
                        .map(
                                row ->
                                        new InvestmentRow(
                                                row.account().id(),
                                                row.account().name(),
                                                row.account().scope(),
                                                row.invested(),
                                                row.currentValue(),
                                                row.gain(),
                                                row.gainRate(),
                                                row.lastValuation() == null
                                                        ? null
                                                        : row.lastValuation().valuationDate(),
                                                row.account().currency(),
                                                row.currentValueInBrl(),
                                                row.gainInBrl()))
                        .toList(),
                report.debts().stream()
                        .map(
                                row ->
                                        new DebtRow(
                                                row.debt().id(),
                                                row.debt().name(),
                                                row.debt().type(),
                                                row.debt().creditor(),
                                                row.currentBalance(),
                                                row.lastBalance() == null
                                                        ? null
                                                        : row.lastBalance().balanceDate()))
                        .toList());
    }

    private PointRow toRow(NetWorthCalculator.Point point) {
        return new PointRow(
                point.month(), point.cash(), point.investments(), point.debts(), point.netWorth());
    }
}
