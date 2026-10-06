package com.lmf.finpro.application.calendar;

import com.lmf.finpro.application.exchangerate.ExchangeRateApplicationService;
import com.lmf.finpro.application.support.HouseholdTaxProfile;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.domain.model.DasSchedule;
import com.lmf.finpro.domain.model.FinancialCalendar;
import com.lmf.finpro.domain.model.RecurringTransaction;
import com.lmf.finpro.domain.model.TaxEstimate;
import com.lmf.finpro.domain.model.TaxRegime;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.domain.port.out.RecurringTransactionRepositoryPort;
import com.lmf.finpro.domain.port.out.TaxEstimateRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Calendário financeiro do mês: transações (sem transferências), ocorrências previstas das
 * recorrências, vencimento do DAS (MEI e Simples Nacional) e tudo o que está atrasado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarApplicationService {

    /** Até quantos anos para trás ou para frente do mês atual o calendário pode ser consultado. */
    public static final int MAX_YEARS_AWAY = 5;

    private final HouseholdTaxProfile householdTaxProfile;
    private final AccountRepositoryPort accountRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final RecurringTransactionRepositoryPort recurringTransactionRepositoryPort;
    private final TaxEstimateRepositoryPort taxEstimateRepositoryPort;
    private final CategoryRepositoryPort categoryRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;
    private final Clock clock;
    private final ExchangeRateApplicationService exchangeRateApplicationService;

    /** Calendário com os nomes de conta, categoria e cliente de cada lançamento já resolvidos. */
    public record Result(
            FinancialCalendar.Report report,
            Map<Long, String> accountNames,
            Map<Long, String> categoryNames,
            Map<Long, String> clientNames) {}

    public Result build(Long currentHouseholdId, YearMonth month, boolean includePaid) {
        log.debug(
                "Montando calendário do usuário={} mês={} incluirPagas={}",
                currentHouseholdId,
                month,
                includePaid);
        LocalDate today = LocalDate.now(clock);
        YearMonth referenceMonth = month == null ? YearMonth.from(today) : month;
        YearMonth current = YearMonth.from(today);
        if (referenceMonth.isBefore(current.minusYears(MAX_YEARS_AWAY))
                || referenceMonth.isAfter(current.plusYears(MAX_YEARS_AWAY))) {
            throw new IllegalArgumentException(
                    "O mês deve estar a até " + MAX_YEARS_AWAY + " anos do mês atual");
        }

        List<Account> accounts = accountRepositoryPort.findAllByHouseholdId(currentHouseholdId);
        List<Long> accountIds = accounts.stream().map(Account::id).toList();
        List<Transaction> transactions =
                accountIds.isEmpty()
                        ? List.of()
                        : transactionRepositoryPort.findAllByAccountIds(accountIds).stream()
                                .filter(transaction -> transaction.transferId() == null)
                                .toList();

        FinancialCalendar.Report report =
                FinancialCalendar.build(
                        referenceMonth,
                        today,
                        transactions,
                        recurrencesInBrl(
                                accounts,
                                recurringTransactionRepositoryPort.findAllByHouseholdId(
                                        currentHouseholdId)),
                        dasDue(currentHouseholdId, referenceMonth),
                        includePaid);

        return new Result(
                report,
                accounts.stream().collect(Collectors.toMap(Account::id, Account::name)),
                categoryRepositoryPort.findAllVisibleToUser(currentHouseholdId).stream()
                        .collect(Collectors.toMap(Category::id, Category::name)),
                clientRepositoryPort.findAllByHouseholdId(currentHouseholdId).stream()
                        .collect(Collectors.toMap(Client::id, Client::name)));
    }

    /** Valores em reais, como as transações ({@link Transaction#baseAmount()}). */
    private List<RecurringTransaction> recurrencesInBrl(
            List<Account> accounts, List<RecurringTransaction> recurrences) {
        return accounts.stream().allMatch(account -> account.currency().isBase())
                ? recurrences
                : exchangeRateApplicationService.recurrencesInBrl(accounts, recurrences);
    }

    /** O DAS que vence no mês (dia 20) é o da competência anterior. */
    private FinancialCalendar.DasDue dasDue(Long householdId, YearMonth month) {
        // O DAS é da pessoa: só aparece no calendário do espaço pessoal.
        TaxRegime regime = householdTaxProfile.regimeOf(householdId);
        if (regime == null || !DasSchedule.appliesTo(regime)) {
            return null;
        }
        YearMonth competence = month.minusMonths(1);
        BigDecimal estimatedValue =
                taxEstimateRepositoryPort.findAllByHouseholdId(householdId).stream()
                        .filter(estimate -> estimate.referenceMonth().equals(competence))
                        .map(TaxEstimate::estimatedValue)
                        .findFirst()
                        .orElse(null);
        return new FinancialCalendar.DasDue(
                competence, DasSchedule.dueDateFor(competence), estimatedValue);
    }
}
