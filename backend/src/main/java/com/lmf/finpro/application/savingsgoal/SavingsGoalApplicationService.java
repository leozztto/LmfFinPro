package com.lmf.finpro.application.savingsgoal;

import com.lmf.finpro.application.FlowLog;
import com.lmf.finpro.application.support.HouseholdTaxProfile;
import com.lmf.finpro.application.transfer.TransferApplicationService;
import com.lmf.finpro.application.transfer.TransferResult;
import com.lmf.finpro.domain.exception.EntityHasLinkedRecordsException;
import com.lmf.finpro.domain.exception.InsufficientBalanceException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.exception.SameAccountTransferException;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.ContributionType;
import com.lmf.finpro.domain.model.GoalContribution;
import com.lmf.finpro.domain.model.SavingsGoal;
import com.lmf.finpro.domain.model.SavingsGoalCalculator;
import com.lmf.finpro.domain.model.TaxRateEstimator;
import com.lmf.finpro.domain.model.TaxRegime;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.GoalContributionRepositoryPort;
import com.lmf.finpro.domain.port.out.SavingsGoalRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Metas de economia ("caixinhas"): o valor guardado fica de verdade numa conta reserva ({@link
 * SavingsGoal#accountId()}), que pode ser compartilhada por várias metas. Aporte e resgate são
 * transferências reais entre essa conta e a conta de origem da meta ({@link
 * SavingsGoal#fundingAccountId()}), via {@link TransferApplicationService}. Todas as contas
 * (guardado, quanto falta, valor mensal até o prazo e a sugestão de quanto separar das receitas)
 * são feitas aqui, não no frontend.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SavingsGoalApplicationService {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MM/yyyy");

    private final SavingsGoalRepositoryPort savingsGoalRepositoryPort;
    private final GoalContributionRepositoryPort goalContributionRepositoryPort;
    private final AccountRepositoryPort accountRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final HouseholdTaxProfile householdTaxProfile;
    private final TransferApplicationService transferApplicationService;
    private final Clock clock;

    public List<SavingsGoalSummary> list(Long currentHouseholdId) {
        log.debug("Listando metas de economia do usuário={}", currentHouseholdId);
        List<SavingsGoal> goals =
                savingsGoalRepositoryPort.findAllByHouseholdId(currentHouseholdId);
        if (goals.isEmpty()) {
            return List.of();
        }
        BigDecimal monthPaidIncome = monthPaidIncome(currentHouseholdId);
        return goals.stream().map(goal -> summarize(goal, monthPaidIncome)).toList();
    }

    public SavingsGoalSummary get(Long currentHouseholdId, Long goalId) {
        log.debug("Buscando meta={} do usuário={}", goalId, currentHouseholdId);
        return summarize(
                findOwnedOrThrow(currentHouseholdId, goalId), monthPaidIncome(currentHouseholdId));
    }

    /**
     * {@code accountId} (conta reserva) e {@code fundingAccountId} (conta de origem) são fixos
     * desde a criação — precisam existir, pertencer ao usuário, ser diferentes entre si e ter a
     * mesma moeda (sem isso, todo aporte/resgate exigiria câmbio, fora do escopo desta meta).
     * {@code accountId} também precisa ser uma conta do tipo {@link AccountType#RESERVE}.
     */
    @Transactional
    public SavingsGoalSummary create(
            Long currentHouseholdId,
            Long accountId,
            Long fundingAccountId,
            SavingsGoalCommand command) {
        log.debug(
                "Criando meta na conta reserva={} com origem={} para o usuário={}",
                accountId,
                fundingAccountId,
                currentHouseholdId);
        requireIncomeRateWhenAutoContribute(command.incomeRate(), command.autoContribute());
        requireValidAccountPair(currentHouseholdId, accountId, fundingAccountId);
        SavingsGoal created =
                savingsGoalRepositoryPort.save(
                        SavingsGoal.create(
                                currentHouseholdId,
                                command.name().trim(),
                                command.type(),
                                command.targetAmount(),
                                command.deadline(),
                                command.incomeRate(),
                                command.autoContribute(),
                                accountId,
                                fundingAccountId));
        return summarize(created, monthPaidIncome(currentHouseholdId));
    }

    public SavingsGoalSummary update(
            Long currentHouseholdId, Long goalId, SavingsGoalCommand command) {
        log.debug("Atualizando meta={} do usuário={}", goalId, currentHouseholdId);
        requireIncomeRateWhenAutoContribute(command.incomeRate(), command.autoContribute());
        SavingsGoal updated =
                savingsGoalRepositoryPort.save(
                        findOwnedOrThrow(currentHouseholdId, goalId)
                                .withDetails(
                                        command.name().trim(),
                                        command.type(),
                                        command.targetAmount(),
                                        command.deadline(),
                                        command.incomeRate(),
                                        command.autoContribute()));
        return summarize(updated, monthPaidIncome(currentHouseholdId));
    }

    /**
     * Só deixa excluir com o valor guardado zerado: o dinheiro é real, então excluir a meta com
     * saldo apagaria o rótulo sem o usuário ter resgatado nada. As transferências já feitas nunca
     * são desfeitas por aqui — só a meta e o histórico de aportes (que já estará vazio de valor).
     */
    public void delete(Long currentHouseholdId, Long goalId) {
        log.debug("Removendo meta={} do usuário={}", goalId, currentHouseholdId);
        findOwnedOrThrow(currentHouseholdId, goalId);
        BigDecimal saved = savedAmount(goalId);
        if (saved.signum() != 0) {
            throw new EntityHasLinkedRecordsException(
                    "Esta meta ainda tem "
                            + saved.setScale(2, RoundingMode.HALF_UP)
                            + " guardado. Resgate tudo antes de excluir.");
        }
        savingsGoalRepositoryPort.deleteById(goalId);
    }

    public List<GoalContribution> listContributions(Long currentHouseholdId, Long goalId) {
        log.debug("Listando aportes da meta={} do usuário={}", goalId, currentHouseholdId);
        findOwnedOrThrow(currentHouseholdId, goalId);
        return goalContributionRepositoryPort.findAllByGoalId(goalId);
    }

    /** Resgate não pode deixar a meta com valor guardado negativo. */
    @Transactional
    public GoalContribution addContribution(
            Long currentHouseholdId,
            Long currentUserId,
            Long goalId,
            ContributionType type,
            BigDecimal amount,
            LocalDate date,
            String note) {
        log.debug("Registrando {} na meta={} do usuário={}", type, goalId, currentHouseholdId);
        SavingsGoal goal = findOwnedOrThrow(currentHouseholdId, goalId);
        if (type == ContributionType.WITHDRAWAL && amount.compareTo(savedAmount(goalId)) > 0) {
            throw new InsufficientBalanceException(
                    "O resgate é maior que o valor guardado na meta");
        }
        return createRealContribution(
                currentHouseholdId, currentUserId, goal, type, amount, date, blankToNull(note));
    }

    /**
     * Excluir um aporte também não pode deixar a meta negativa (se já houve resgates). A
     * transferência real por trás do aporte é desfeita junto: a linha de {@code goal_contributions}
     * some sozinha (FK {@code ON DELETE CASCADE} em {@code transfer_id}).
     */
    @Transactional
    public void deleteContribution(
            Long currentHouseholdId, Long currentUserId, Long goalId, Long contributionId) {
        log.debug(
                "Removendo aporte={} da meta={} do usuário={}",
                contributionId,
                goalId,
                currentHouseholdId);
        findOwnedOrThrow(currentHouseholdId, goalId);
        GoalContribution contribution =
                goalContributionRepositoryPort
                        .findById(contributionId)
                        .filter(found -> found.goalId().equals(goalId))
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Aporte não encontrado: " + contributionId));
        if (savedAmount(goalId).subtract(contribution.signedAmount()).signum() < 0) {
            throw new InsufficientBalanceException(
                    "Excluir este aporte deixaria a meta com valor guardado negativo");
        }
        transferApplicationService.delete(
                currentHouseholdId, currentUserId, contribution.transferId());
    }

    /** Lança como aporte o valor sugerido pelo percentual da meta (o "separar com 1 clique"). */
    @Transactional
    public GoalContribution applySuggestion(
            Long currentHouseholdId, Long currentUserId, Long goalId) {
        log.debug(
                "Aplicando sugestão de aporte na meta={} do usuário={}",
                goalId,
                currentHouseholdId);
        SavingsGoalSummary summary = get(currentHouseholdId, goalId);
        BigDecimal suggested = summary.suggestedContribution();
        if (suggested == null || suggested.signum() == 0) {
            throw new IllegalArgumentException("Não há valor a separar nesta meta agora");
        }
        return createSuggestedContribution(currentHouseholdId, currentUserId, summary, false);
    }

    public List<SavingsGoal> findAllAutoContribute() {
        return savingsGoalRepositoryPort.findAllAutoContribute();
    }

    /**
     * Mesma sugestão do "separar com 1 clique", aplicada automaticamente pelo scheduler para quem
     * ligou o aporte automático — silenciosamente não faz nada quando não há valor a separar agora
     * (o comum na maioria dos dias, não é um erro) ou quando a conta de origem não tem saldo
     * suficiente para a transferência (fica pra um próximo dia, sem quebrar os outros). Como a
     * sugestão já desconta o que foi aportado no mês, rodar isso todo dia captura incrementalmente
     * a fração de cada receita nova que chega.
     */
    @Transactional
    public boolean applyAutomaticContributionIfDue(SavingsGoal goal) {
        SavingsGoalSummary summary = summarize(goal, monthPaidIncome(goal.householdId()));
        BigDecimal suggested = summary.suggestedContribution();
        FlowLog.detail("goalId", goal.id());
        boolean contributed = suggested != null && suggested.signum() > 0;
        if (contributed) {
            // Aporte automático (agendado): sem usuário que age, então sem autor.
            createSuggestedContribution(goal.householdId(), null, summary, true);
        }
        FlowLog.detail("contributed", contributed);
        return contributed;
    }

    private GoalContribution createSuggestedContribution(
            Long currentHouseholdId,
            Long currentUserId,
            SavingsGoalSummary summary,
            boolean automatic) {
        LocalDate today = LocalDate.now(clock);
        String note =
                "Separação%s de %s%% das receitas recebidas em %s"
                        .formatted(
                                automatic ? " automática" : "",
                                percent(summary.goal().incomeRate()),
                                YearMonth.from(today).format(MONTH));
        return createRealContribution(
                currentHouseholdId,
                currentUserId,
                summary.goal(),
                ContributionType.DEPOSIT,
                summary.suggestedContribution(),
                today,
                note);
    }

    /**
     * Monta a transferência real (aporte: origem → reserva; resgate: reserva → origem),
     * reaproveitando {@link TransferApplicationService} para a validação de saldo e a criação das
     * duas transações da transferência, e grava o aporte/resgate com o id dela.
     */
    private GoalContribution createRealContribution(
            Long currentHouseholdId,
            Long currentUserId,
            SavingsGoal goal,
            ContributionType type,
            BigDecimal amount,
            LocalDate date,
            String note) {
        boolean isDeposit = type == ContributionType.DEPOSIT;
        Long fromAccountId = isDeposit ? goal.fundingAccountId() : goal.accountId();
        Long toAccountId = isDeposit ? goal.accountId() : goal.fundingAccountId();
        String description =
                (isDeposit ? "Aporte na meta \"" : "Resgate da meta \"") + goal.name() + "\"";
        TransferResult transfer =
                transferApplicationService.create(
                        currentHouseholdId,
                        currentUserId,
                        fromAccountId,
                        toAccountId,
                        amount,
                        date,
                        description,
                        null);
        return goalContributionRepositoryPort.save(
                GoalContribution.create(
                        goal.id(), type, amount, date, note, transfer.transfer().id()));
    }

    /**
     * Percentual sugerido para a caixinha do imposto: a alíquota de referência do regime do usuário
     * ({@link TaxRateEstimator}), sobre a receita média dos 3 meses anteriores — o mês atual ainda
     * está pela metade e puxaria a faixa do autônomo para baixo.
     */
    public BigDecimal suggestedTaxRate(Long currentHouseholdId) {
        log.debug("Calculando alíquota sugerida para o usuário={}", currentHouseholdId);
        TaxRegime regime = householdTaxProfile.regimeOf(currentHouseholdId);
        if (regime == null) {
            return BigDecimal.ZERO;
        }
        YearMonth currentMonth = YearMonth.from(LocalDate.now(clock));
        BigDecimal lastThreeMonths =
                sumIncome(
                        currentHouseholdId,
                        transaction -> {
                            YearMonth month = YearMonth.from(transaction.transactionDate());
                            return month.isBefore(currentMonth)
                                    && !month.isBefore(currentMonth.minusMonths(3));
                        });
        BigDecimal averageMonthly =
                lastThreeMonths.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
        return TaxRateEstimator.suggestRate(regime, averageMonthly);
    }

    private SavingsGoalSummary summarize(SavingsGoal goal, BigDecimal monthPaidIncome) {
        List<GoalContribution> contributions =
                goalContributionRepositoryPort.findAllByGoalId(goal.id());
        LocalDate today = LocalDate.now(clock);
        YearMonth currentMonth = YearMonth.from(today);
        BigDecimal saved = SavingsGoalCalculator.savedAmount(contributions);
        BigDecimal monthDeposits =
                contributions.stream()
                        .filter(contribution -> contribution.type() == ContributionType.DEPOSIT)
                        .filter(
                                contribution ->
                                        YearMonth.from(contribution.contributionDate())
                                                .equals(currentMonth))
                        .map(GoalContribution::amount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SavingsGoalSummary(
                goal,
                saved,
                SavingsGoalCalculator.remaining(goal, saved),
                SavingsGoalCalculator.monthlyNeeded(goal, saved, today),
                monthPaidIncome,
                SavingsGoalCalculator.suggestedContribution(
                        goal, saved, monthPaidIncome, monthDeposits));
    }

    private BigDecimal savedAmount(Long goalId) {
        return SavingsGoalCalculator.savedAmount(
                goalContributionRepositoryPort.findAllByGoalId(goalId));
    }

    /** Receitas já recebidas (pagas) no mês atual, sem transferências entre contas próprias. */
    private BigDecimal monthPaidIncome(Long householdId) {
        YearMonth currentMonth = YearMonth.from(LocalDate.now(clock));
        return sumIncome(
                householdId,
                transaction ->
                        transaction.isPaid()
                                && YearMonth.from(transaction.transactionDate())
                                        .equals(currentMonth));
    }

    private BigDecimal sumIncome(Long householdId, Predicate<Transaction> filter) {
        List<Long> accountIds =
                accountRepositoryPort.findAllByHouseholdId(householdId).stream()
                        .map(Account::id)
                        .toList();
        if (accountIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return transactionRepositoryPort.findAllByAccountIds(accountIds).stream()
                .filter(transaction -> transaction.type() == CategoryType.INCOME)
                .filter(transaction -> transaction.transferId() == null)
                .filter(filter)
                .map(Transaction::baseAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void requireIncomeRateWhenAutoContribute(
            BigDecimal incomeRate, boolean autoContribute) {
        if (autoContribute && (incomeRate == null || incomeRate.signum() == 0)) {
            throw new IllegalArgumentException(
                    "Para ligar o aporte automático, defina o percentual das receitas a separar.");
        }
    }

    private void requireValidAccountPair(
            Long currentHouseholdId, Long accountId, Long fundingAccountId) {
        if (accountId.equals(fundingAccountId)) {
            throw new SameAccountTransferException(
                    "A conta reserva e a conta de origem não podem ser a mesma.");
        }
        Account account = findAccountOrThrow(currentHouseholdId, accountId);
        Account fundingAccount = findAccountOrThrow(currentHouseholdId, fundingAccountId);
        if (account.type() != AccountType.RESERVE) {
            throw new IllegalArgumentException(
                    "A conta reserva precisa ser do tipo Conta reserva.");
        }
        if (account.currency() != fundingAccount.currency()) {
            throw new IllegalArgumentException(
                    "A conta reserva e a conta de origem precisam ter a mesma moeda.");
        }
    }

    private Account findAccountOrThrow(Long currentHouseholdId, Long accountId) {
        return accountRepositoryPort
                .findById(accountId)
                .filter(account -> account.belongsTo(currentHouseholdId))
                .orElseThrow(
                        () -> new ResourceNotFoundException("Conta não encontrada: " + accountId));
    }

    /** Acesso a meta de outro usuário é tratado como inexistente (404), não como 403. */
    private SavingsGoal findOwnedOrThrow(Long currentHouseholdId, Long goalId) {
        return savingsGoalRepositoryPort
                .findById(goalId)
                .filter(goal -> goal.belongsTo(currentHouseholdId))
                .orElseThrow(() -> new ResourceNotFoundException("Meta não encontrada: " + goalId));
    }

    private static String percent(BigDecimal rate) {
        return rate.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
