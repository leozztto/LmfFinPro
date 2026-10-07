package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.calendar.CalendarApplicationService;
import com.lmf.finpro.domain.model.FinancialCalendar;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.calendar.CalendarResponse;
import com.lmf.finpro.infrastructure.web.dto.calendar.CalendarResponse.DayRow;
import com.lmf.finpro.infrastructure.web.dto.calendar.CalendarResponse.EntryRow;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Calendário do mês: lançamentos futuros, vencimentos e atrasados. */
@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarApplicationService calendarApplicationService;

    /** Sem {@code month}, usa o mês atual. */
    @GetMapping
    public CalendarResponse calendar(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam(defaultValue = "false") boolean includePaid) {
        CalendarApplicationService.Result result =
                calendarApplicationService.build(currentUser.householdId(), month, includePaid);
        FinancialCalendar.Report report = result.report();
        return new CalendarResponse(
                report.month(),
                report.today(),
                report.expectedIncome(),
                report.expectedExpense(),
                report.paidIncome(),
                report.paidExpense(),
                report.overdueIncome(),
                report.overdueExpense(),
                report.days().stream()
                        .map(
                                day ->
                                        new DayRow(
                                                day.date(),
                                                day.income(),
                                                day.expense(),
                                                day.entries().stream()
                                                        .map(entry -> toRow(entry, result))
                                                        .toList()))
                        .toList(),
                report.overdue().stream().map(entry -> toRow(entry, result)).toList());
    }

    private EntryRow toRow(FinancialCalendar.Entry entry, CalendarApplicationService.Result names) {
        return new EntryRow(
                entry.kind().name(),
                entry.date(),
                entry.description(),
                entry.amount(),
                entry.type().name(),
                entry.status().name(),
                entry.transactionId(),
                entry.recurringTransactionId(),
                entry.accountId() == null ? null : names.accountNames().get(entry.accountId()),
                entry.categoryId() == null ? null : names.categoryNames().get(entry.categoryId()),
                entry.clientId() == null ? null : names.clientNames().get(entry.clientId()),
                entry.competence());
    }
}
