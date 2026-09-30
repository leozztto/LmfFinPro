package com.lmf.finpro.application.savingsgoal;

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
import com.lmf.finpro.domain.port.out.UserRepositoryPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
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
@Service
@RequiredArgsConstructor
public class SavingsGoalApplicationService {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MM/yyyy");

    private final SavingsGoalRepositoryPort savingsGoalRepositoryPort;
    private final GoalContributionRepositoryPort goalContributionRepositoryPort;
    private final AccountRepositoryPort accountRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final TransferApplicationService transferApplicationService;
    private final Clock clock;

    public List<SavingsGoalSummary> list(Long currentUserId) {
        List<SavingsGoal> goals = savingsGoalRepositoryPort.findAllByUserId(currentUserId);
        if (goals.isEmpty()) {
            return List.of();
        }
        BigDecimal monthPaidIncome = monthPaidIncome(currentUserId);
        return goals.stream().map(goal -> summarize(goal, monthPaidIncome)).toList();
    }

    public SavingsGoalSummary get(Long currentUserId, Long goalId) {
        return summarize(findOwnedOrThrow(currentUserId, goalId), monthPaidIncome(currentUserId));
    }

    /**
     * {@code accountId} (conta reserva) e {@code fundingAccountId} (conta de origem) são fixos
     * desde a criação — precisam existir, pertencer ao usuário, ser diferentes entre si e ter a
     * mesma moeda (sem isso, todo aporte/resgate exigiria câmbio, fora do escopo desta meta).
     * {@code accountId} também precisa ser uma conta do tipo {@link AccountType#RESERVE}.
     */
    @Transactional
    public SavingsGoalSummary create(
            Long currentUserId, Long accountId, Long fundingAccountId, SavingsGoalCommand command) {
        requireIncomeRateWhenAutoContribute(command.incomeRate(), command.autoContribute());
        requireValidAccountPair(currentUserId, accountId, fundingAccountId);
        SavingsGoal created =
                savingsGoalRepositoryPort.save(
                        SavingsGoal.create(
                                currentUserId,
                                command.name().trim(),
                                command.type(),
                                command.targetAmount(),
                                command.deadline(),
                                command.incomeRate(),
                                command.autoContribute(),
                                accountId,
                                fundingAccountId));
        return summarize(created, monthPaidIncome(currentUserId));
    }

    public SavingsGoalSummary update(Long currentUserId, Long goalId, SavingsGoalCommand command) {
        requireIncomeRateWhenAutoContribute(command.incomeRate(), command.autoContribute());
        SavingsGoal updated =
                savingsGoalRepositoryPort.save(
                        findOwnedOrThrow(currentUserId, goalId)
                                .withDetails(
                                        command.name().trim(),
                                        command.type(),
                                        command.targetAmount(),
                                        command.deadline(),
                                        command.incomeRate(),
                                        command.autoContribute()));
        return summarize(updated, monthPaidIncome(currentUserId));
    }

    /**
     * Só deixa excluir com o valor guardado zerado: o dinheiro é real, então excluir a meta com
     * saldo apagaria o rótulo sem o usuário ter resgatado nada. As transferências já feitas nunca
     * são desfeitas por aqui — só a meta e o histórico de aportes (que já estará vazio de valor).
     */
    public void delete(Long currentUserId, Long goalId) {
        findOwnedOrThrow(currentUserId, goalId);
        BigDecimal saved = savedAmount(goalId);
        if (saved.signum() != 0) {
            throw new EntityHasLinkedRecordsException(
                    "Esta meta ainda tem "
                            + saved.setScale(2, RoundingMode.HALF_UP)
                            + " guardado. Resgate tudo antes de excluir.");
        }
        savingsGoalRepositoryPort.deleteById(goalId);
    }

    public List<GoalContribution> listContributions(Long currentUserId, Long goalId) {
        findOwnedOrThrow(currentUserId, goalId);
        return goalContributionRepositoryPort.findAllByGoalId(goalId);
    }

    /** Resgate não pode deixar a meta com valor guardado negativo. */
    @Transactional
    public GoalContribution addContribution(
            Long currentUserId,
            Long goalId,
            ContributionType type,
            BigDecimal amount,
            LocalDate date,
            String note) {
        SavingsGoal goal = findOwnedOrThrow(currentUserId, goalId);
        if (type == ContributionType.WITHDRAWAL && amount.compareTo(savedAmount(goalId)) > 0) {
            throw new InsufficientBalanceException(
                    "O resgate é maior que o valor guardado na meta");
        }
        return createRealContribution(currentUserId, goal, type, amount, date, blankToNull(note));
    }

    /**
     * Excluir um aporte também não pode deixar a meta negativa (se já houve resgates). A
     * transferência real por trás do aporte é desfeita junto: a linha de {@code goal_contributions}
     * some sozinha (FK {@code ON DELETE CASCADE} em {@code transfer_id}).
     */
    @Transactional
    public void deleteContribution(Long currentUserId, Long goalId, Long contributionId) {
        findOwnedOrThrow(currentUserId, goalId);
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
        transferApplicationService.delete(currentUserId, contribution.transferId());
    }

    /** Lança como aporte o valor sugerido pelo percentual da meta (o "separar com 1 clique"). */
    @Transactional
    public GoalContribution applySuggestion(Long currentUserId, Long goalId) {
        SavingsGoalSummary summary = get(currentUserId, goalId);
        BigDecimal suggested = summary.suggestedContribution();
        if (suggested == null || suggested.signum() == 0) {
            throw new IllegalArgumentException("Não há valor a separar nesta meta agora");
        }
        return createSuggestedContribution(currentUserId, summary, false);
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
    public void applyAutomaticContributionIfDue(SavingsGoal goal) {
        SavingsGoalSummary summary = summarize(goal, monthPaidIncome(goal.userId()));
        BigDecimal suggested = summary.suggestedContribution();
        if (suggested != null && suggested.signum() > 0) {
            createSuggestedContribution(goal.userId(), summary, true);
        }
    }

    private GoalContribution createSuggestedContribution(
            Long currentUserId, SavingsGoalSummary summary, boolean automatic) {
        LocalDate today = LocalDate.now(clock);
        String note =
                "Separação%s de %s%% das receitas recebidas em %s"
                        .formatted(
                                automatic ? " automática" : "",
                                percent(summary.goal().incomeRate()),
                                YearMonth.from(today).format(MONTH));
        return createRealContribution(
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
                        currentUserId, fromAccountId, toAccountId, amount, date, description);
        return goalContributionRepositoryPort.save(
                GoalContribution.create(
                        goal.id(), type, amount, date, note, transfer.transfer().id()));
    }

    /**
     * Percentual sugerido para a caixinha do imposto: a alíquota de referência do regime do usuário
     * ({@link TaxRateEstimator}), sobre a receita média dos 3 meses anteriores — o mês atual ainda
     * está pela metade e puxaria a faixa do autônomo para baixo.
     */
    public BigDecimal suggestedTaxRate(Long currentUserId) {
        TaxRegime regime =
                userRepositoryPort
                        .findById(currentUserId)
                        .map(user -> user.taxRegime())
                        .orElse(null);
        if (regime == null) {
            return BigDecimal.ZERO;
        }
        YearMonth currentMonth = YearMonth.from(LocalDate.now(clock));
        BigDecimal lastThreeMonths =
                sumIncome(
                        currentUserId,
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
    private BigDecimal monthPaidIncome(Long userId) {
        YearMonth currentMonth = YearMonth.from(LocalDate.now(clock));
        return sumIncome(
                userId,
                transaction ->
                        transaction.isPaid()
                                && YearMonth.from(transaction.transactionDate())
                                        .equals(currentMonth));
    }

    private BigDecimal sumIncome(Long userId, Predicate<Transaction> filter) {
        List<Long> accountIds =
                accountRepositoryPort.findAllByUserId(userId).stream().map(Account::id).toList();
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
            Long currentUserId, Long accountId, Long fundingAccountId) {
        if (accountId.equals(fundingAccountId)) {
            throw new SameAccountTransferException(
                    "A conta reserva e a conta de origem não podem ser a mesma.");
        }
        Account account = findAccountOrThrow(currentUserId, accountId);
        Account fundingAccount = findAccountOrThrow(currentUserId, fundingAccountId);
        if (account.type() != AccountType.RESERVE) {
            throw new IllegalArgumentException(
                    "A conta reserva precisa ser do tipo Conta reserva.");
        }
        if (account.currency() != fundingAccount.currency()) {
            throw new IllegalArgumentException(
                    "A conta reserva e a conta de origem precisam ter a mesma moeda.");
        }
    }

    private Account findAccountOrThrow(Long currentUserId, Long accountId) {
        return accountRepositoryPort
                .findById(accountId)
                .filter(account -> account.belongsTo(currentUserId))
                .orElseThrow(
                        () -> new ResourceNotFoundException("Conta não encontrada: " + accountId));
    }

    /** Acesso a meta de outro usuário é tratado como inexistente (404), não como 403. */
    private SavingsGoal findOwnedOrThrow(Long currentUserId, Long goalId) {
        return savingsGoalRepositoryPort
                .findById(goalId)
                .filter(goal -> goal.belongsTo(currentUserId))
                .orElseThrow(() -> new ResourceNotFoundException("Meta não encontrada: " + goalId));
    }

    private static String percent(BigDecimal rate) {
        return rate.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
