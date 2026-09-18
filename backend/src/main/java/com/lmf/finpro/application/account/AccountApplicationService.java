package com.lmf.finpro.application.account;

import com.lmf.finpro.domain.exception.AccountHasLinkedRecordsException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import com.lmf.finpro.domain.port.out.TransferRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountApplicationService {

    private final AccountRepositoryPort accountRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final TransferRepositoryPort transferRepositoryPort;

    public Account create(Long currentUserId, String name, AccountType type, BigDecimal initialBalance) {
        return accountRepositoryPort.save(Account.create(currentUserId, name, type, initialBalance));
    }

    public List<Account> list(Long currentUserId) {
        return accountRepositoryPort.findAllByUserId(currentUserId);
    }

    public Account getById(Long currentUserId, Long accountId) {
        return findOwnedOrThrow(currentUserId, accountId);
    }

    public Account update(Long currentUserId, Long accountId, String name, AccountType type, BigDecimal initialBalance) {
        Account existing = findOwnedOrThrow(currentUserId, accountId);
        return accountRepositoryPort.save(existing.withDetails(name, type, initialBalance));
    }

    public void delete(Long currentUserId, Long accountId) {
        findOwnedOrThrow(currentUserId, accountId);
        if (transactionRepositoryPort.existsByAccountId(accountId) || transferRepositoryPort.existsByAccountId(accountId)) {
            throw new AccountHasLinkedRecordsException(
                "Esta conta possui transações ou transferências vinculadas. Exclua-as antes de remover a conta."
            );
        }
        accountRepositoryPort.deleteById(accountId);
    }

    /**
     * Saldo atual = saldo inicial + receitas - despesas lançadas naquela conta. Não há saldo
     * persistido: é recalculado a cada leitura para nunca ficar dessincronizado das transações
     * (que podem ser editadas ou excluídas depois de lançadas).
     */
    public BigDecimal calculateCurrentBalance(Account account) {
        BigDecimal income = transactionRepositoryPort.sumAmountByAccountIdAndType(account.id(), CategoryType.INCOME);
        BigDecimal expense = transactionRepositoryPort.sumAmountByAccountIdAndType(account.id(), CategoryType.EXPENSE);
        return account.initialBalance().add(income).subtract(expense);
    }

    /** Acesso a conta de outro usuário é tratado como inexistente (404), não como 403. */
    private Account findOwnedOrThrow(Long currentUserId, Long accountId) {
        return accountRepositoryPort.findById(accountId)
            .filter(account -> account.belongsTo(currentUserId))
            .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada: " + accountId));
    }
}
