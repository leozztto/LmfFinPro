package com.lmf.finpro.application.report;

import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.domain.model.ClientAnnualStatementData;
import com.lmf.finpro.domain.model.ClientReceiptData;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Monta os dados dos documentos de um cliente: recibo mensal e demonstrativo anual. */
@Component
@RequiredArgsConstructor
class ClientReportDataFactory {

    private final ReportLookups lookups;
    private final TransactionRepositoryPort transactionRepositoryPort;

    /**
     * Soma as receitas do cliente em cada um dos 12 meses do ano (mesmo os que ficarem zerados),
     * reaproveitando {@code findAllByClientIdAndTypeAndDateBetween} com o intervalo do ano inteiro
     * em vez de um mês, igual ao recibo mensal.
     */
    ClientAnnualStatementData buildAnnualStatement(
            Long currentHouseholdId, Long currentUserId, Long clientId, Year year) {
        Client client = lookups.findOwnedClientOrThrow(currentHouseholdId, clientId);
        User issuer = lookups.findUserOrThrow(currentUserId);

        LocalDate start = year.atDay(1);
        LocalDate end = year.plusYears(1).atDay(1);
        List<Transaction> transactions =
                transactionRepositoryPort.findAllByClientIdAndTypeAndDateBetween(
                        clientId, CategoryType.INCOME, start, end);

        Map<Month, BigDecimal> totalsByMonth = new EnumMap<>(Month.class);
        for (Month month : Month.values()) {
            totalsByMonth.put(month, BigDecimal.ZERO);
        }
        for (Transaction transaction : transactions) {
            totalsByMonth.merge(
                    transaction.transactionDate().getMonth(),
                    transaction.baseAmount(),
                    BigDecimal::add);
        }

        List<ClientAnnualStatementData.MonthlyIncome> monthlyIncomes =
                Arrays.stream(Month.values())
                        .map(
                                month ->
                                        new ClientAnnualStatementData.MonthlyIncome(
                                                month, totalsByMonth.get(month)))
                        .toList();

        BigDecimal totalYear =
                transactions.stream()
                        .map(Transaction::baseAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ClientAnnualStatementData(issuer, client, year, monthlyIncomes, totalYear);
    }

    ClientReceiptData buildReceipt(
            Long currentHouseholdId, Long currentUserId, Long clientId, YearMonth referenceMonth) {
        Client client = lookups.findOwnedClientOrThrow(currentHouseholdId, clientId);
        User issuer = lookups.findUserOrThrow(currentUserId);

        LocalDate start = referenceMonth.atDay(1);
        LocalDate end = referenceMonth.plusMonths(1).atDay(1);
        List<Transaction> transactions =
                transactionRepositoryPort.findAllByClientIdAndTypeAndDateBetween(
                        clientId, CategoryType.INCOME, start, end);

        BigDecimal total =
                transactions.stream()
                        .map(Transaction::baseAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ClientReceiptData(issuer, client, referenceMonth, transactions, total);
    }
}
