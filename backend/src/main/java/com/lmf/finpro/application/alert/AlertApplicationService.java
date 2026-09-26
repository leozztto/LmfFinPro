package com.lmf.finpro.application.alert;

import com.lmf.finpro.application.budget.BudgetApplicationService;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AlertDigest;
import com.lmf.finpro.domain.model.AlertDigest.BillDue;
import com.lmf.finpro.domain.model.AlertDigest.BudgetAlert;
import com.lmf.finpro.domain.model.AlertDigest.DasReminder;
import com.lmf.finpro.domain.model.AlertType;
import com.lmf.finpro.domain.model.Budget;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.DasSchedule;
import com.lmf.finpro.domain.model.NotificationPreferences;
import com.lmf.finpro.domain.model.SentAlert;
import com.lmf.finpro.domain.model.TaxEstimate;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.AlertMailerPort;
import com.lmf.finpro.domain.port.out.BudgetRepositoryPort;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.NotificationPreferencesRepositoryPort;
import com.lmf.finpro.domain.port.out.SentAlertRepositoryPort;
import com.lmf.finpro.domain.port.out.TaxEstimateRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import com.lmf.finpro.domain.port.out.UserRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Monta e envia o resumo diário de alertas: contas a vencer, orçamentos do mês que passaram de 80%
 * ou 100% e o lembrete do DAS. Cada aviso é enviado uma única vez — o que já foi avisado fica em
 * {@code sent_alerts} e não entra nos resumos seguintes.
 */
@Service
@RequiredArgsConstructor
public class AlertApplicationService {

    private static final BigDecimal WARNING_RATIO = new BigDecimal("0.8");

    private final UserRepositoryPort userRepositoryPort;
    private final NotificationPreferencesRepositoryPort notificationPreferencesRepositoryPort;
    private final SentAlertRepositoryPort sentAlertRepositoryPort;
    private final AccountRepositoryPort accountRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final BudgetRepositoryPort budgetRepositoryPort;
    private final BudgetApplicationService budgetApplicationService;
    private final CategoryRepositoryPort categoryRepositoryPort;
    private final TaxEstimateRepositoryPort taxEstimateRepositoryPort;
    private final AlertMailerPort alertMailerPort;
    private final Clock clock;

    public List<Long> findAllRecipientIds() {
        return userRepositoryPort.findAllIds();
    }

    /**
     * Envia o resumo do usuário, se houver algo novo a avisar. Os avisos só são registrados como
     * enviados depois do envio: se o e-mail falhar, eles entram de novo no resumo do dia seguinte.
     *
     * @return se um e-mail foi enviado
     */
    @Transactional
    public boolean sendAlertsTo(Long userId) {
        return userRepositoryPort.findById(userId).map(this::sendAlertsTo).orElse(false);
    }

    boolean sendAlertsTo(User user) {
        LocalDate today = LocalDate.now(clock);
        NotificationPreferences preferences =
                notificationPreferencesRepositoryPort
                        .findByUserId(user.id())
                        .orElseGet(() -> NotificationPreferences.defaults(user.id()));

        List<SentAlert> toRecord = new ArrayList<>();
        List<BillDue> bills =
                preferences.billsEnabled()
                        ? collectBills(user.id(), today, preferences.billDaysBefore(), toRecord)
                        : List.of();
        List<BudgetAlert> budgets =
                preferences.budgetsEnabled()
                        ? collectBudgets(user.id(), YearMonth.from(today), toRecord)
                        : List.of();
        DasReminder das =
                preferences.dasEnabled()
                        ? collectDas(user, today, preferences.billDaysBefore(), toRecord)
                        : null;

        AlertDigest digest = new AlertDigest(bills, budgets, das);
        if (digest.isEmpty()) {
            return false;
        }
        alertMailerPort.sendDigest(user.email(), user.name(), digest);
        toRecord.forEach(sentAlertRepositoryPort::save);
        return true;
    }

    /** Despesas pendentes (sem transferências) com vencimento entre hoje e hoje + N dias. */
    private List<BillDue> collectBills(
            Long userId, LocalDate today, int daysBefore, List<SentAlert> toRecord) {
        List<Long> accountIds =
                accountRepositoryPort.findAllByUserId(userId).stream().map(Account::id).toList();
        if (accountIds.isEmpty()) {
            return List.of();
        }
        LocalDate windowEnd = today.plusDays(daysBefore);
        List<Transaction> due =
                transactionRepositoryPort.findAllByAccountIds(accountIds).stream()
                        .filter(transaction -> transaction.type() == CategoryType.EXPENSE)
                        .filter(transaction -> !transaction.isPaid())
                        .filter(transaction -> transaction.transferId() == null)
                        .filter(transaction -> !transaction.transactionDate().isBefore(today))
                        .filter(transaction -> !transaction.transactionDate().isAfter(windowEnd))
                        .filter(
                                transaction ->
                                        !alreadySent(
                                                userId,
                                                AlertType.BILL_DUE,
                                                transaction.id().toString()))
                        .sorted(Comparator.comparing(Transaction::transactionDate))
                        .toList();

        List<BillDue> bills = new ArrayList<>();
        for (Transaction transaction : due) {
            bills.add(
                    new BillDue(
                            transaction.description(),
                            transaction.amount(),
                            transaction.transactionDate()));
            toRecord.add(new SentAlert(userId, AlertType.BILL_DUE, transaction.id().toString()));
        }
        return bills;
    }

    /**
     * Orçamentos do mês atual que atingiram 80% ou 100% do limite (mesmo gasto da tela de
     * orçamentos, pagas e pendentes). Ao passar direto para 100%, o aviso de 80% também é marcado
     * como enviado, para não chegar depois um "passou de 80%" de um orçamento já estourado.
     */
    private List<BudgetAlert> collectBudgets(
            Long userId, YearMonth currentMonth, List<SentAlert> toRecord) {
        List<BudgetAlert> alerts = new ArrayList<>();
        for (Budget budget : budgetRepositoryPort.findAllByUserId(userId)) {
            if (!budget.referenceMonth().equals(currentMonth)
                    || budget.limitValue().signum() <= 0) {
                continue;
            }
            String key = budget.id().toString();
            if (alreadySent(userId, AlertType.BUDGET_100, key)) {
                continue;
            }
            BigDecimal spent = budgetApplicationService.calculateSpent(budget);
            boolean sent80 = alreadySent(userId, AlertType.BUDGET_80, key);

            int threshold;
            if (spent.compareTo(budget.limitValue()) >= 0) {
                threshold = 100;
                toRecord.add(new SentAlert(userId, AlertType.BUDGET_100, key));
                if (!sent80) {
                    toRecord.add(new SentAlert(userId, AlertType.BUDGET_80, key));
                }
            } else if (!sent80
                    && spent.compareTo(budget.limitValue().multiply(WARNING_RATIO)) >= 0) {
                threshold = 80;
                toRecord.add(new SentAlert(userId, AlertType.BUDGET_80, key));
            } else {
                continue;
            }
            alerts.add(
                    new BudgetAlert(
                            categoryName(budget.categoryId()),
                            budget.limitValue(),
                            spent,
                            threshold));
        }
        return alerts;
    }

    /** Lembrete do DAS (MEI e Simples Nacional) quando o dia 20 entra na janela de antecedência. */
    private DasReminder collectDas(
            User user, LocalDate today, int daysBefore, List<SentAlert> toRecord) {
        if (!DasSchedule.appliesTo(user.taxRegime())) {
            return null;
        }
        YearMonth competence = DasSchedule.competenceDueWithin(today, daysBefore).orElse(null);
        if (competence == null
                || alreadySent(user.id(), AlertType.DAS_DUE, competence.toString())) {
            return null;
        }
        BigDecimal estimatedValue =
                taxEstimateRepositoryPort.findAllByUserId(user.id()).stream()
                        .filter(estimate -> estimate.referenceMonth().equals(competence))
                        .map(TaxEstimate::estimatedValue)
                        .findFirst()
                        .orElse(null);
        toRecord.add(new SentAlert(user.id(), AlertType.DAS_DUE, competence.toString()));
        return new DasReminder(competence, DasSchedule.dueDateFor(competence), estimatedValue);
    }

    private boolean alreadySent(Long userId, AlertType type, String key) {
        return sentAlertRepositoryPort.exists(userId, type, key);
    }

    private String categoryName(Long categoryId) {
        return categoryRepositoryPort.findById(categoryId).map(Category::name).orElse("Categoria");
    }
}
