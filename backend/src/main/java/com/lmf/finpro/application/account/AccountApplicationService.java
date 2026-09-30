package com.lmf.finpro.application.account;

import com.lmf.finpro.domain.exception.CurrencyChangeNotAllowedException;
import com.lmf.finpro.domain.exception.EntityHasLinkedRecordsException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountBalances;
import com.lmf.finpro.domain.model.AccountScope;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.AccountValuation;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Currency;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.AccountValuationRepositoryPort;
import com.lmf.finpro.domain.port.out.RecurringTransactionRepositoryPort;
import com.lmf.finpro.domain.port.out.SavingsGoalRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import com.lmf.finpro.domain.port.out.TransferRepositoryPort;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountApplicationService {

    private final AccountRepositoryPort accountRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final TransferRepositoryPort transferRepositoryPort;
    private final RecurringTransactionRepositoryPort recurringTransactionRepositoryPort;
    private final AccountValuationRepositoryPort accountValuationRepositoryPort;
    private final SavingsGoalRepositoryPort savingsGoalRepositoryPort;

    public Account create(
            Long currentUserId,
            String name,
            AccountType type,
            BigDecimal initialBalance,
            AccountScope scope) {
        return create(currentUserId, name, type, initialBalance, scope, null);
    }

    /** Sem uso informado, a conta é pessoal (PF); sem moeda, em reais. */
    public Account create(
            Long currentUserId,
            String name,
            AccountType type,
            BigDecimal initialBalance,
            AccountScope scope,
            Currency currency) {
        return accountRepositoryPort.save(
                Account.create(
                        currentUserId,
                        name,
                        type,
                        initialBalance,
                        scope == null ? AccountScope.PERSONAL : scope,
                        currency == null ? Currency.BRL : currency));
    }

    public List<Account> list(Long currentUserId) {
        return accountRepositoryPort.findAllByUserId(currentUserId);
    }

    public Account getById(Long currentUserId, Long accountId) {
        return findOwnedOrThrow(currentUserId, accountId);
    }

    public Account update(
            Long currentUserId, Long accountId, String name, AccountType type, AccountScope scope) {
        return update(currentUserId, accountId, name, type, scope, null);
    }

    /**
     * Saldo inicial só é definido na criação da conta: a edição não pode alterá-lo, para não
     * distorcer o histórico de saldo. Sem uso ou moeda informados, mantém os atuais. A moeda só
     * muda enquanto nada foi lançado na conta: os valores já lançados estão na moeda antiga.
     */
    public Account update(
            Long currentUserId,
            Long accountId,
            String name,
            AccountType type,
            AccountScope scope,
            Currency currency) {
        Account existing = findOwnedOrThrow(currentUserId, accountId);
        Currency newCurrency = currency == null ? existing.currency() : currency;
        if (newCurrency != existing.currency() && hasLinkedRecords(accountId)) {
            throw new CurrencyChangeNotAllowedException(
                    "Esta conta já tem lançamentos na moeda atual. Para usar outra moeda, crie uma nova conta.");
        }
        if (existing.type() == AccountType.INVESTMENT
                && type != AccountType.INVESTMENT
                && accountValuationRepositoryPort.existsByAccountId(accountId)) {
            throw new EntityHasLinkedRecordsException(
                    "Esta conta de investimento tem valores de mercado informados. Exclua-os antes de mudar o tipo da conta.");
        }
        if (existing.type() == AccountType.RESERVE
                && type != AccountType.RESERVE
                && savingsGoalRepositoryPort.existsByAccountId(accountId)) {
            throw new EntityHasLinkedRecordsException(
                    "Esta conta é a conta reserva de uma meta de economia. Exclua a meta antes de mudar o tipo da conta.");
        }
        return accountRepositoryPort.save(
                existing.withDetails(
                        name,
                        type,
                        existing.initialBalance(),
                        scope == null ? existing.scope() : scope,
                        newCurrency));
    }

    /** Conta com algo lançado: transações, transferências, recorrências ou valores de mercado. */
    public boolean hasLinkedRecords(Long accountId) {
        return transactionRepositoryPort.existsByAccountId(accountId)
                || transferRepositoryPort.existsByAccountId(accountId)
                || recurringTransactionRepositoryPort.existsByAccountId(accountId)
                || accountValuationRepositoryPort.existsByAccountId(accountId);
    }

    public void delete(Long currentUserId, Long accountId) {
        findOwnedOrThrow(currentUserId, accountId);
        if (transactionRepositoryPort.existsByAccountId(accountId)
                || transferRepositoryPort.existsByAccountId(accountId)) {
            throw new EntityHasLinkedRecordsException(
                    "Esta conta possui transações ou transferências vinculadas. Exclua-as antes de remover a conta.");
        }
        if (recurringTransactionRepositoryPort.existsByAccountId(accountId)) {
            throw new EntityHasLinkedRecordsException(
                    "Esta conta possui lançamentos recorrentes vinculados. Exclua-os antes de remover a conta.");
        }
        if (savingsGoalRepositoryPort.existsByAccountId(accountId)) {
            throw new EntityHasLinkedRecordsException(
                    "Esta conta possui metas de economia vinculadas. Exclua-as antes de remover a conta.");
        }
        accountRepositoryPort.deleteById(accountId);
    }

    /**
     * Saldo atual = saldo inicial + receitas - despesas já pagas naquela conta (pendentes ficam de
     * fora até serem confirmadas). Não há saldo persistido: é recalculado a cada leitura para nunca
     * ficar dessincronizado das transações (que podem ser editadas ou excluídas depois de
     * lançadas).
     *
     * <p>Conta de investimento com valor de mercado informado: o último valor mais as movimentações
     * posteriores a ele ({@link AccountBalances}).
     */
    public BigDecimal calculateCurrentBalance(Account account) {
        if (account.type() == AccountType.INVESTMENT) {
            List<AccountValuation> valuations =
                    accountValuationRepositoryPort.findAllByAccountId(account.id());
            if (!valuations.isEmpty()) {
                return AccountBalances.balance(
                        account,
                        transactionRepositoryPort.findAllByAccountIds(List.of(account.id())),
                        valuations,
                        null);
            }
        }
        BigDecimal income =
                transactionRepositoryPort.sumPaidAmountByAccountIdAndType(
                        account.id(), CategoryType.INCOME);
        BigDecimal expense =
                transactionRepositoryPort.sumPaidAmountByAccountIdAndType(
                        account.id(), CategoryType.EXPENSE);
        return account.initialBalance().add(income).subtract(expense);
    }

    /** Acesso a conta de outro usuário é tratado como inexistente (404), não como 403. */
    private Account findOwnedOrThrow(Long currentUserId, Long accountId) {
        return accountRepositoryPort
                .findById(accountId)
                .filter(account -> account.belongsTo(currentUserId))
                .orElseThrow(
                        () -> new ResourceNotFoundException("Conta não encontrada: " + accountId));
    }
}
