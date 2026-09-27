package com.lmf.finpro.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AccountBalancesTest {

    private static final Account INVESTMENT = account(1L, AccountType.INVESTMENT, "0");
    private static final Account CHECKING = account(2L, AccountType.CHECKING, "100");

    @Test
    void withoutValuationUsesTheBookBalance() {
        List<Transaction> transactions =
                List.of(
                        paid(1L, CategoryType.INCOME, "1000", "2026-01-10"),
                        paid(1L, CategoryType.EXPENSE, "200", "2026-02-10"),
                        pending(1L, CategoryType.INCOME, "999", "2026-02-11"));

        assertThat(AccountBalances.balance(INVESTMENT, transactions, List.of(), null))
                .isEqualByComparingTo("800");
        assertThat(
                        AccountBalances.balance(
                                INVESTMENT, transactions, List.of(), LocalDate.of(2026, 1, 31)))
                .isEqualByComparingTo("1000");
    }

    @Test
    void investmentFollowsTheLastValuationPlusLaterMovements() {
        // Aplicou 1000, valia 1100 em 31/01, aplicou mais 500 e resgatou 300 depois.
        List<Transaction> transactions =
                List.of(
                        paid(1L, CategoryType.INCOME, "1000", "2026-01-10"),
                        paid(1L, CategoryType.INCOME, "500", "2026-02-05"),
                        paid(1L, CategoryType.EXPENSE, "300", "2026-03-05"));
        List<AccountValuation> valuations = List.of(valuation(1L, "2026-01-31", "1100"));

        assertThat(AccountBalances.balance(INVESTMENT, transactions, valuations, null))
                .isEqualByComparingTo("1300");
        assertThat(
                        AccountBalances.balance(
                                INVESTMENT, transactions, valuations, LocalDate.of(2026, 2, 28)))
                .isEqualByComparingTo("1600");
        // Antes da valorização vale o saldo contábil.
        assertThat(
                        AccountBalances.balance(
                                INVESTMENT, transactions, valuations, LocalDate.of(2026, 1, 20)))
                .isEqualByComparingTo("1000");
        assertThat(AccountBalances.valuationGain(INVESTMENT, transactions, valuations, null))
                .isEqualByComparingTo("100");
    }

    @Test
    void sameDayMovementCountsOnlyIfRegisteredAfterTheValuation() {
        LocalDateTime informedAt = LocalDateTime.of(2026, 9, 27, 10, 0);
        List<AccountValuation> valuations =
                List.of(
                        new AccountValuation(
                                null,
                                1L,
                                LocalDate.of(2026, 9, 27),
                                new BigDecimal("1100"),
                                informedAt));
        Transaction before =
                withCreatedAt(
                        paid(1L, CategoryType.INCOME, "1000", "2026-09-27"),
                        informedAt.minusHours(1));
        Transaction after =
                withCreatedAt(
                        paid(1L, CategoryType.EXPENSE, "1100", "2026-09-27"),
                        informedAt.plusMinutes(5));

        assertThat(AccountBalances.balance(INVESTMENT, List.of(before), valuations, null))
                .isEqualByComparingTo("1100");
        assertThat(AccountBalances.balance(INVESTMENT, List.of(before, after), valuations, null))
                .isEqualByComparingTo("0");
    }

    @Test
    void usesTheLatestValuationUpToTheDate() {
        List<Transaction> transactions =
                List.of(paid(1L, CategoryType.INCOME, "1000", "2026-01-10"));
        List<AccountValuation> valuations =
                List.of(
                        valuation(1L, "2026-03-31", "1080"),
                        valuation(1L, "2026-01-31", "1010"),
                        valuation(9L, "2026-04-30", "5000"));

        assertThat(
                        AccountBalances.balance(
                                INVESTMENT, transactions, valuations, LocalDate.of(2026, 2, 28)))
                .isEqualByComparingTo("1010");
        assertThat(AccountBalances.balance(INVESTMENT, transactions, valuations, null))
                .isEqualByComparingTo("1080");
    }

    @Test
    void otherAccountTypesIgnoreValuations() {
        List<Transaction> transactions = List.of(paid(2L, CategoryType.INCOME, "50", "2026-01-10"));

        assertThat(
                        AccountBalances.balance(
                                CHECKING,
                                transactions,
                                List.of(valuation(2L, "2026-01-31", "9999")),
                                null))
                .isEqualByComparingTo("150");
        assertThat(
                        AccountBalances.valuationGain(
                                CHECKING,
                                transactions,
                                List.of(valuation(2L, "2026-01-31", "9999")),
                                null))
                .isEqualByComparingTo("0");
    }

    @Test
    void initialBalanceCountsOnlyFromTheAccountCreation() {
        Account createdInSeptember =
                new Account(
                        3L,
                        10L,
                        "Conta",
                        AccountType.CHECKING,
                        new BigDecimal("5000"),
                        LocalDateTime.of(2026, 9, 27, 15, 0));

        assertThat(
                        AccountBalances.balance(
                                createdInSeptember,
                                List.of(),
                                List.of(),
                                LocalDate.of(2026, 8, 31)))
                .isEqualByComparingTo("0");
        assertThat(
                        AccountBalances.balance(
                                createdInSeptember,
                                List.of(),
                                List.of(),
                                LocalDate.of(2026, 9, 27)))
                .isEqualByComparingTo("5000");
        assertThat(AccountBalances.balance(createdInSeptember, List.of(), List.of(), null))
                .isEqualByComparingTo("5000");
    }

    private static Account account(Long id, AccountType type, String initialBalance) {
        return new Account(id, 10L, "Conta", type, new BigDecimal(initialBalance), null);
    }

    private static AccountValuation valuation(Long accountId, String date, String value) {
        return new AccountValuation(
                null, accountId, LocalDate.parse(date), new BigDecimal(value), null);
    }

    private static Transaction withCreatedAt(Transaction transaction, LocalDateTime createdAt) {
        return new Transaction(
                transaction.id(),
                transaction.accountId(),
                null,
                null,
                transaction.description(),
                transaction.amount(),
                transaction.transactionDate(),
                transaction.type(),
                transaction.origin(),
                createdAt,
                null,
                null,
                null,
                transaction.status());
    }

    private static Transaction paid(Long accountId, CategoryType type, String amount, String date) {
        return transaction(accountId, type, amount, date, TransactionStatus.PAID);
    }

    private static Transaction pending(
            Long accountId, CategoryType type, String amount, String date) {
        return transaction(accountId, type, amount, date, TransactionStatus.PENDING);
    }

    private static Transaction transaction(
            Long accountId,
            CategoryType type,
            String amount,
            String date,
            TransactionStatus status) {
        return new Transaction(
                null,
                accountId,
                null,
                null,
                "Movimento",
                new BigDecimal(amount),
                LocalDate.parse(date),
                type,
                TransactionOrigin.MANUAL,
                null,
                null,
                null,
                null,
                status);
    }
}
