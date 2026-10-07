package com.lmf.finpro.application.alert;

import com.lmf.finpro.application.FlowLog;
import com.lmf.finpro.application.budget.BudgetApplicationService;
import com.lmf.finpro.application.push.PushNotificationApplicationService;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AlertDigest;
import com.lmf.finpro.domain.model.AlertDigest.BillDue;
import com.lmf.finpro.domain.model.AlertDigest.BudgetAlert;
import com.lmf.finpro.domain.model.AlertDigest.DasReminder;
import com.lmf.finpro.domain.model.AlertDigest.OverdueBill;
import com.lmf.finpro.domain.model.AlertDigest.RecurringBudgetExpiring;
import com.lmf.finpro.domain.model.AlertType;
import com.lmf.finpro.domain.model.Budget;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.DasSchedule;
import com.lmf.finpro.domain.model.HouseholdMembership;
import com.lmf.finpro.domain.model.NotificationPreferences;
import com.lmf.finpro.domain.model.RecurringBudget;
import com.lmf.finpro.domain.model.SentAlert;
import com.lmf.finpro.domain.model.TaxEstimate;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.AlertMailerPort;
import com.lmf.finpro.domain.port.out.BudgetRepositoryPort;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.HouseholdRepositoryPort;
import com.lmf.finpro.domain.port.out.NotificationPreferencesRepositoryPort;
import com.lmf.finpro.domain.port.out.RecurringBudgetRepositoryPort;
import com.lmf.finpro.domain.port.out.SentAlertRepositoryPort;
import com.lmf.finpro.domain.port.out.TaxEstimateRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import com.lmf.finpro.domain.port.out.UserRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Monta e envia o resumo diário de alertas: contas a vencer, orçamentos do mês que passaram de 80%
 * ou 100%, orçamentos recorrentes perto de expirar e o lembrete do DAS. Cada aviso é enviado uma
 * única vez — o que já foi avisado fica em {@code sent_alerts} e não entra nos resumos seguintes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertApplicationService {

    private static final BigDecimal WARNING_RATIO = new BigDecimal("0.8");

    /** Até quantos dias de atraso uma conta ainda entra no aviso de contas atrasadas. */
    static final int MAX_OVERDUE_DAYS = 30;

    private final UserRepositoryPort userRepositoryPort;
    private final HouseholdRepositoryPort householdRepositoryPort;
    private final NotificationPreferencesRepositoryPort notificationPreferencesRepositoryPort;
    private final SentAlertRepositoryPort sentAlertRepositoryPort;
    private final AccountRepositoryPort accountRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final BudgetRepositoryPort budgetRepositoryPort;
    private final BudgetApplicationService budgetApplicationService;
    private final RecurringBudgetRepositoryPort recurringBudgetRepositoryPort;
    private final CategoryRepositoryPort categoryRepositoryPort;
    private final TaxEstimateRepositoryPort taxEstimateRepositoryPort;
    private final AlertMailerPort alertMailerPort;
    private final PushNotificationApplicationService pushNotificationApplicationService;
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
        // Os dados (contas, orçamentos) são dos grupos de que o usuário participa, o pessoal e os
        // compartilhados; preferências e envio são do usuário. O DAS é da pessoa (regime
        // tributário), então só olha o espaço pessoal.
        List<Long> householdIds =
                householdRepositoryPort.findMembershipsByUserId(user.id()).stream()
                        .map(HouseholdMembership::householdId)
                        .toList();
        if (householdIds.isEmpty()) {
            return false;
        }
        Long personalHouseholdId =
                householdRepositoryPort
                        .findPersonalMembership(user.id())
                        .map(HouseholdMembership::householdId)
                        .orElse(null);
        NotificationPreferences preferences =
                notificationPreferencesRepositoryPort
                        .findByUserId(user.id())
                        .orElseGet(() -> NotificationPreferences.defaults(user.id()));

        List<SentAlert> toRecord = new ArrayList<>();
        List<Transaction> pending =
                preferences.billsEnabled() ? pendingExpenses(householdIds) : List.of();
        List<OverdueBill> overdueBills = collectOverdueBills(user.id(), today, pending, toRecord);
        List<BillDue> bills =
                collectBills(user.id(), today, preferences.billDaysBefore(), pending, toRecord);
        List<BudgetAlert> budgets =
                preferences.budgetsEnabled()
                        ? collectBudgets(user.id(), householdIds, YearMonth.from(today), toRecord)
                        : List.of();
        DasReminder das =
                preferences.dasEnabled() && personalHouseholdId != null
                        ? collectDas(
                                user,
                                personalHouseholdId,
                                today,
                                preferences.billDaysBefore(),
                                toRecord)
                        : null;
        List<RecurringBudgetExpiring> recurringBudgetsExpiring =
                preferences.recurringBudgetsEnabled()
                        ? collectRecurringBudgetsExpiring(
                                user.id(), householdIds, YearMonth.from(today), toRecord)
                        : List.of();

        AlertDigest digest =
                new AlertDigest(bills, overdueBills, budgets, das, recurringBudgetsExpiring);
        FlowLog.detail("userId", user.id());
        if (digest.isEmpty()) {
            FlowLog.detail("digestSent", false);
            return false;
        }
        alertMailerPort.sendDigest(user.email(), user.name(), digest);
        pushNotificationApplicationService.sendDigest(user.id(), digest);
        toRecord.forEach(sentAlertRepositoryPort::save);
        FlowLog.detail("digestSent", true);
        FlowLog.detail("bills", bills.size());
        FlowLog.detail("overdueBills", overdueBills.size());
        FlowLog.detail("budgets", budgets.size());
        FlowLog.detail("das", das != null);
        FlowLog.detail("recurringBudgetsExpiring", recurringBudgetsExpiring.size());
        // Só quando há o que avisar: o scheduler roda um fluxo por usuário em DEBUG e a maioria
        // não tem nada novo, então esta é a única linha em INFO que diz quem recebeu o resumo.
        log.info(
                "Resumo de alertas enviado userId={} contas={} atrasadas={} orçamentos={} das={}"
                        + " recorrentes={}",
                user.id(),
                bills.size(),
                overdueBills.size(),
                budgets.size(),
                das != null,
                recurringBudgetsExpiring.size());
        return true;
    }

    /**
     * Despesas pendentes (sem transferências) do usuário: a base das contas a vencer e atrasadas.
     */
    private List<Transaction> pendingExpenses(List<Long> householdIds) {
        List<Long> accountIds =
                householdIds.stream()
                        .flatMap(id -> accountRepositoryPort.findAllByHouseholdId(id).stream())
                        .map(Account::id)
                        .toList();
        if (accountIds.isEmpty()) {
            return List.of();
        }
        return transactionRepositoryPort.findAllByAccountIds(accountIds).stream()
                .filter(transaction -> transaction.type() == CategoryType.EXPENSE)
                .filter(transaction -> !transaction.isPaid())
                .filter(transaction -> transaction.transferId() == null)
                .toList();
    }

    /**
     * Despesas pendentes que venceram antes de hoje, até {@link #MAX_OVERDUE_DAYS} dias atrás (as
     * mais antigas já não são novidade e despejariam um monte de avisos de uma vez). Cada conta é
     * avisada uma única vez, quando passa a constar como atrasada; a mais atrasada vem primeiro.
     */
    private List<OverdueBill> collectOverdueBills(
            Long userId, LocalDate today, List<Transaction> pending, List<SentAlert> toRecord) {
        LocalDate oldest = today.minusDays(MAX_OVERDUE_DAYS);
        List<Transaction> overdue =
                pending.stream()
                        .filter(transaction -> transaction.transactionDate().isBefore(today))
                        .filter(transaction -> !transaction.transactionDate().isBefore(oldest))
                        .filter(
                                transaction ->
                                        !alreadySent(
                                                userId,
                                                AlertType.BILL_OVERDUE,
                                                transaction.id().toString()))
                        .sorted(Comparator.comparing(Transaction::transactionDate))
                        .toList();

        List<OverdueBill> bills = new ArrayList<>();
        for (Transaction transaction : overdue) {
            bills.add(
                    new OverdueBill(
                            transaction.description(),
                            transaction.baseAmount(),
                            transaction.transactionDate(),
                            ChronoUnit.DAYS.between(transaction.transactionDate(), today)));
            toRecord.add(
                    new SentAlert(userId, AlertType.BILL_OVERDUE, transaction.id().toString()));
        }
        return bills;
    }

    /** Despesas pendentes com vencimento entre hoje e hoje + N dias. */
    private List<BillDue> collectBills(
            Long userId,
            LocalDate today,
            int daysBefore,
            List<Transaction> pending,
            List<SentAlert> toRecord) {
        LocalDate windowEnd = today.plusDays(daysBefore);
        List<Transaction> due =
                pending.stream()
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
                            transaction.baseAmount(),
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
            Long userId,
            List<Long> householdIds,
            YearMonth currentMonth,
            List<SentAlert> toRecord) {
        List<BudgetAlert> alerts = new ArrayList<>();
        List<Budget> budgetsOfAllHouseholds =
                householdIds.stream()
                        .flatMap(id -> budgetRepositoryPort.findAllByHouseholdId(id).stream())
                        .toList();
        for (Budget budget : budgetsOfAllHouseholds) {
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

    /**
     * Orçamentos recorrentes ativos cujo {@code endMonth} é o mês atual ou o próximo — um aviso
     * único por recorrência e mês final, para o usuário decidir se estende antes dela parar de
     * lançar. Estender o {@code endMonth} depois de avisado libera um novo aviso mais pra frente,
     * já que a chave inclui o mês final.
     */
    private List<RecurringBudgetExpiring> collectRecurringBudgetsExpiring(
            Long userId,
            List<Long> householdIds,
            YearMonth currentMonth,
            List<SentAlert> toRecord) {
        List<RecurringBudgetExpiring> alerts = new ArrayList<>();
        List<RecurringBudget> recurrencesOfAllHouseholds =
                householdIds.stream()
                        .flatMap(
                                id ->
                                        recurringBudgetRepositoryPort
                                                .findAllByHouseholdId(id)
                                                .stream())
                        .toList();
        for (RecurringBudget recurrence : recurrencesOfAllHouseholds) {
            YearMonth endMonth = recurrence.endMonth();
            if (!recurrence.active() || endMonth == null) {
                continue;
            }
            boolean expiringSoon =
                    !currentMonth.isBefore(endMonth.minusMonths(1))
                            && !currentMonth.isAfter(endMonth);
            if (!expiringSoon) {
                continue;
            }
            String key = recurrence.id() + "-" + endMonth;
            if (alreadySent(userId, AlertType.RECURRING_BUDGET_EXPIRING, key)) {
                continue;
            }
            alerts.add(
                    new RecurringBudgetExpiring(categoryName(recurrence.categoryId()), endMonth));
            toRecord.add(new SentAlert(userId, AlertType.RECURRING_BUDGET_EXPIRING, key));
        }
        return alerts;
    }

    /** Lembrete do DAS (MEI e Simples Nacional) quando o dia 20 entra na janela de antecedência. */
    private DasReminder collectDas(
            User user,
            Long householdId,
            LocalDate today,
            int daysBefore,
            List<SentAlert> toRecord) {
        if (!DasSchedule.appliesTo(user.taxRegime())) {
            return null;
        }
        YearMonth competence = DasSchedule.competenceDueWithin(today, daysBefore).orElse(null);
        if (competence == null
                || alreadySent(user.id(), AlertType.DAS_DUE, competence.toString())) {
            return null;
        }
        BigDecimal estimatedValue =
                taxEstimateRepositoryPort.findAllByHouseholdId(householdId).stream()
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
