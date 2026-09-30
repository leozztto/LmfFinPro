package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/** Relatório de patrimônio pronto para os testes dos geradores de PDF e CSV. */
public final class NetWorthReportTestData {

    private NetWorthReportTestData() {}

    /** Dois meses, uma conta em dólar, um investimento com rendimento e uma dívida. */
    public static NetWorthCalculator.Report report() {
        Account checking =
                new Account(
                        1L,
                        10L,
                        "Conta; Corrente",
                        AccountType.CHECKING,
                        BigDecimal.ZERO,
                        LocalDateTime.now());
        Account dollars =
                new Account(
                        2L,
                        10L,
                        "Conta EUA",
                        AccountType.CHECKING,
                        BigDecimal.ZERO,
                        LocalDateTime.now(),
                        AccountScope.PERSONAL,
                        Currency.USD);
        Account broker =
                new Account(
                        3L,
                        10L,
                        "Corretora",
                        AccountType.INVESTMENT,
                        BigDecimal.ZERO,
                        LocalDateTime.now());
        Debt car = new Debt(4L, 10L, "Carro", DebtType.FINANCING, "Banco X", LocalDateTime.now());

        List<NetWorthCalculator.Point> history =
                List.of(
                        new NetWorthCalculator.Point(
                                YearMonth.of(2026, 8),
                                new BigDecimal("1000.00"),
                                new BigDecimal("1000.00"),
                                new BigDecimal("400.00"),
                                new BigDecimal("1600.00")),
                        new NetWorthCalculator.Point(
                                YearMonth.of(2026, 9),
                                new BigDecimal("1500.00"),
                                new BigDecimal("1100.00"),
                                new BigDecimal("300.00"),
                                new BigDecimal("2300.00")));
        return new NetWorthCalculator.Report(
                history.get(1),
                new BigDecimal("700.00"),
                new BigDecimal("100.00"),
                history,
                List.of(
                        new NetWorthCalculator.AccountRow(
                                checking, new BigDecimal("1000.00"), new BigDecimal("1000.00")),
                        new NetWorthCalculator.AccountRow(
                                dollars, new BigDecimal("100.00"), new BigDecimal("500.00"))),
                List.of(
                        new NetWorthCalculator.InvestmentRow(
                                broker,
                                new BigDecimal("1000.00"),
                                new BigDecimal("1100.00"),
                                new BigDecimal("100.00"),
                                new BigDecimal("0.1000"),
                                AccountValuation.create(
                                        3L, LocalDate.of(2026, 9, 28), new BigDecimal("1100.00")),
                                new BigDecimal("1100.00"),
                                new BigDecimal("100.00"))),
                List.of(
                        new NetWorthCalculator.DebtRow(
                                car,
                                new BigDecimal("300.00"),
                                DebtBalance.create(
                                        4L, LocalDate.of(2026, 9, 10), new BigDecimal("300.00")))));
    }

    /** Sem nada cadastrado: só o ponto do mês atual, zerado. */
    public static NetWorthCalculator.Report emptyReport() {
        NetWorthCalculator.Point zero =
                new NetWorthCalculator.Point(
                        YearMonth.of(2026, 9),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO);
        return new NetWorthCalculator.Report(
                zero, null, BigDecimal.ZERO, List.of(zero), List.of(), List.of(), List.of());
    }
}
