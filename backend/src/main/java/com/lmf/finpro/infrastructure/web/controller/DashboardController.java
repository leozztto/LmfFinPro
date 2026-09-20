package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.dashboard.DashboardApplicationService;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.dashboard.BalancePointResponse;
import com.lmf.finpro.infrastructure.web.dto.dashboard.BreakdownPointResponse;
import com.lmf.finpro.infrastructure.web.dto.dashboard.CashFlowProjectionPointResponse;
import com.lmf.finpro.infrastructure.web.dto.dashboard.DashboardOverviewResponse;
import com.lmf.finpro.infrastructure.web.dto.dashboard.MonthlyFlowPointResponse;
import com.lmf.finpro.infrastructure.web.mapper.DashboardWebMapper;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de agregação do dashboard — um por gráfico, para que o frontend busque e renderize cada
 * um de forma independente (sem esperar todos os dados para mostrar o primeiro gráfico).
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardApplicationService dashboardApplicationService;
    private final DashboardWebMapper mapper;

    @GetMapping("/overview")
    public DashboardOverviewResponse overview(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return mapper.toResponse(dashboardApplicationService.getOverview(currentUser.userId()));
    }

    @GetMapping("/monthly-flow")
    public List<MonthlyFlowPointResponse> monthlyFlow(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "6") int months) {
        return dashboardApplicationService.getMonthlyFlow(currentUser.userId(), months).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @GetMapping("/balance-evolution")
    public List<BalancePointResponse> balanceEvolution(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "6") int months) {
        return dashboardApplicationService
                .getBalanceEvolution(currentUser.userId(), months)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @GetMapping("/cash-flow-projection")
    public List<CashFlowProjectionPointResponse> cashFlowProjection(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "3") int months) {
        return dashboardApplicationService
                .getCashFlowProjection(currentUser.userId(), months)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @GetMapping("/category-breakdown")
    public List<BreakdownPointResponse> categoryBreakdown(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam CategoryType type,
            @RequestParam String month) {
        return dashboardApplicationService
                .getCategoryBreakdown(currentUser.userId(), type, parseMonth(month))
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @GetMapping("/client-breakdown")
    public List<BreakdownPointResponse> clientBreakdown(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @RequestParam String month) {
        return dashboardApplicationService
                .getClientBreakdown(currentUser.userId(), parseMonth(month))
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("mês inválido, use o formato aaaa-MM");
        }
    }
}
