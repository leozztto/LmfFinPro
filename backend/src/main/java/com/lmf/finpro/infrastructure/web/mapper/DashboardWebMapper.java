package com.lmf.finpro.infrastructure.web.mapper;

import com.lmf.finpro.domain.model.BalancePoint;
import com.lmf.finpro.domain.model.BreakdownPoint;
import com.lmf.finpro.domain.model.CashFlowProjectionPoint;
import com.lmf.finpro.domain.model.DashboardOverview;
import com.lmf.finpro.domain.model.MonthlyFlowPoint;
import com.lmf.finpro.infrastructure.web.dto.dashboard.BalancePointResponse;
import com.lmf.finpro.infrastructure.web.dto.dashboard.BreakdownPointResponse;
import com.lmf.finpro.infrastructure.web.dto.dashboard.CashFlowProjectionPointResponse;
import com.lmf.finpro.infrastructure.web.dto.dashboard.DashboardOverviewResponse;
import com.lmf.finpro.infrastructure.web.dto.dashboard.MonthlyFlowPointResponse;
import org.springframework.stereotype.Component;

@Component
public class DashboardWebMapper {

    public DashboardOverviewResponse toResponse(DashboardOverview overview) {
        return new DashboardOverviewResponse(
            overview.currentBalance(), overview.currentMonthIncome(), overview.currentMonthExpense(),
            overview.balanceDeltaPercent(), overview.incomeDeltaPercent(), overview.expenseDeltaPercent()
        );
    }

    public MonthlyFlowPointResponse toResponse(MonthlyFlowPoint point) {
        return new MonthlyFlowPointResponse(point.month(), point.income(), point.expense());
    }

    public BalancePointResponse toResponse(BalancePoint point) {
        return new BalancePointResponse(point.month(), point.balance());
    }

    public CashFlowProjectionPointResponse toResponse(CashFlowProjectionPoint point) {
        return new CashFlowProjectionPointResponse(point.month(), point.balance(), point.projected());
    }

    public BreakdownPointResponse toResponse(BreakdownPoint point) {
        return new BreakdownPointResponse(point.entityId(), point.value());
    }
}
