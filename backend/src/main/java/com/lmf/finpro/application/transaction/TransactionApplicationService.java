package com.lmf.finpro.application.transaction;

import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.exception.TransactionLinkedToTransferException;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionApplicationService {

    private final TransactionRepositoryPort transactionRepositoryPort;
    private final AccountRepositoryPort accountRepositoryPort;
    private final CategoryRepositoryPort categoryRepositoryPort;

    public Transaction create(
        Long currentUserId, Long accountId, Long categoryId, Long clientId,
        String description, BigDecimal amount, LocalDate transactionDate, CategoryType type
    ) {
        requireOwnedAccount(currentUserId, accountId);
        requireVisibleCategoryIfPresent(currentUserId, categoryId);
        return transactionRepositoryPort.save(
            Transaction.create(accountId, categoryId, clientId, description, amount, transactionDate, type)
        );
    }

    public List<Transaction> list(Long currentUserId) {
        List<Long> ownedAccountIds = accountRepositoryPort.findAllByUserId(currentUserId).stream()
            .map(Account::id)
            .toList();
        return transactionRepositoryPort.findAllByAccountIds(ownedAccountIds);
    }

    public Transaction getById(Long currentUserId, Long transactionId) {
        return findOwnedOrThrow(currentUserId, transactionId);
    }

    public Transaction update(
        Long currentUserId, Long transactionId, Long categoryId, Long clientId,
        String description, BigDecimal amount, LocalDate transactionDate, CategoryType type
    ) {
        Transaction existing = findOwnedOrThrow(currentUserId, transactionId);
        requireVisibleCategoryIfPresent(currentUserId, categoryId);
        return transactionRepositoryPort.save(
            existing.withDetails(categoryId, clientId, description, amount, transactionDate, type)
        );
    }

    public void delete(Long currentUserId, Long transactionId) {
        Transaction existing = findOwnedOrThrow(currentUserId, transactionId);
        if (existing.transferId() != null) {
            throw new TransactionLinkedToTransferException(
                "Esta transação faz parte de uma transferência. Exclua a transferência inteira na tela de Transferências."
            );
        }
        transactionRepositoryPort.deleteById(transactionId);
    }

    private Transaction findOwnedOrThrow(Long currentUserId, Long transactionId) {
        Transaction transaction = transactionRepositoryPort.findById(transactionId)
            .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada: " + transactionId));
        requireOwnedAccount(currentUserId, transaction.accountId());
        return transaction;
    }

    private void requireOwnedAccount(Long currentUserId, Long accountId) {
        accountRepositoryPort.findById(accountId)
            .filter(account -> account.belongsTo(currentUserId))
            .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada: " + accountId));
    }

    private void requireVisibleCategoryIfPresent(Long currentUserId, Long categoryId) {
        if (categoryId == null) {
            return;
        }
        categoryRepositoryPort.findById(categoryId)
            .filter(category -> category.isVisibleTo(currentUserId))
            .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada: " + categoryId));
    }
}
