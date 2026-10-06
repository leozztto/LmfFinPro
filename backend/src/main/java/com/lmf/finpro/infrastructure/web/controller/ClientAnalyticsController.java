package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.client.ClientAnalyticsApplicationService;
import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.domain.model.ClientAnalytics;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.client.ClientAnalyticsResponse;
import com.lmf.finpro.infrastructure.web.dto.client.ClientAnalyticsResponse.ClientRow;
import com.lmf.finpro.infrastructure.web.dto.client.ClientAnalyticsResponse.MonthRow;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Histórico, ranking e concentração de receita por cliente nos últimos N meses. */
@RestController
@RequestMapping("/api/clients/analytics")
@RequiredArgsConstructor
public class ClientAnalyticsController {

    private final ClientAnalyticsApplicationService clientAnalyticsApplicationService;

    @GetMapping
    public ClientAnalyticsResponse analytics(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "12") int months,
            @RequestParam(defaultValue = "false") boolean onlyReceived) {
        ClientAnalyticsApplicationService.Result result =
                clientAnalyticsApplicationService.analyze(
                        currentUser.householdId(), months, onlyReceived);
        ClientAnalytics.Report report = result.report();
        return new ClientAnalyticsResponse(
                report.months(),
                report.totalIncome(),
                report.unassignedIncome(),
                report.activeClients(),
                report.averageTicket(),
                report.topClientShare(),
                report.topThreeShare(),
                report.risk().name(),
                report.ranking().stream()
                        .map(
                                summary ->
                                        toRow(
                                                summary,
                                                result.clientsById().get(summary.clientId())))
                        .toList());
    }

    private ClientRow toRow(ClientAnalytics.ClientSummary summary, Client client) {
        return new ClientRow(
                summary.clientId(),
                client == null ? "Cliente removido" : client.name(),
                client == null ? null : client.color(),
                summary.income(),
                summary.expense(),
                summary.net(),
                summary.incomeCount(),
                summary.averageTicket(),
                summary.share(),
                summary.activeMonths(),
                summary.lastIncomeDate(),
                summary.monthly().stream()
                        .map(month -> new MonthRow(month.month(), month.income(), month.expense()))
                        .toList());
    }
}
