package com.lmf.finpro.application.transaction;

import com.lmf.finpro.domain.exception.CategoryTypeMismatchException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.exception.TransactionLinkedToTransferException;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.TransactionStatus;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionApplicationService {

    private final TransactionRepositoryPort transactionRepositoryPort;
    private final AccountRepositoryPort accountRepositoryPort;
    private final CategoryRepositoryPort categoryRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;
    private final Clock clock;

    /**
     * {@code status} nulo usa o padrão pela data: data futura fica pendente, o resto já nasce pago.
     */
    public Transaction create(
            Long currentUserId,
            Long accountId,
            Long categoryId,
            Long clientId,
            String description,
            BigDecimal amount,
            LocalDate transactionDate,
            CategoryType type,
            TransactionStatus status) {
        requireOwnedAccount(currentUserId, accountId);
        requireMatchingCategoryTypeIfPresent(currentUserId, categoryId, type);
        requireOwnedClientIfPresent(currentUserId, clientId);
        TransactionStatus resolvedStatus =
                status != null
                        ? status
                        : TransactionStatus.defaultFor(transactionDate, LocalDate.now(clock));
        return transactionRepositoryPort.save(
                Transaction.create(
                        accountId,
                        categoryId,
                        clientId,
                        description,
                        amount,
                        transactionDate,
                        type,
                        resolvedStatus));
    }

    public List<Transaction> list(Long currentUserId) {
        List<Long> ownedAccountIds =
                accountRepositoryPort.findAllByUserId(currentUserId).stream()
                        .map(Account::id)
                        .toList();
        return transactionRepositoryPort.findAllByAccountIds(ownedAccountIds);
    }

    public Transaction getById(Long currentUserId, Long transactionId) {
        return findOwnedOrThrow(currentUserId, transactionId);
    }

    public Transaction update(
            Long currentUserId,
            Long transactionId,
            Long categoryId,
            Long clientId,
            String description,
            BigDecimal amount,
            LocalDate transactionDate,
            CategoryType type,
            TransactionStatus status) {
        Transaction existing = findOwnedOrThrow(currentUserId, transactionId);
        requireMatchingCategoryTypeIfPresent(currentUserId, categoryId, type);
        requireOwnedClientIfPresent(currentUserId, clientId);
        Transaction updated =
                existing.withDetails(
                        categoryId, clientId, description, amount, transactionDate, type);
        if (status != null) {
            requireStatusChangeAllowed(existing, status);
            updated = updated.withStatus(status);
        }
        return transactionRepositoryPort.save(updated);
    }

    /** Marca como paga ou pendente — a ação rápida da lista de transações. */
    public Transaction updateStatus(
            Long currentUserId, Long transactionId, TransactionStatus status) {
        Transaction existing = findOwnedOrThrow(currentUserId, transactionId);
        requireStatusChangeAllowed(existing, status);
        return transactionRepositoryPort.save(existing.withStatus(status));
    }

    /**
     * Transferência já movimentou o dinheiro nas duas contas: não existe transferência pendente.
     */
    private void requireStatusChangeAllowed(Transaction transaction, TransactionStatus status) {
        if (transaction.transferId() != null && status != TransactionStatus.PAID) {
            throw new TransactionLinkedToTransferException(
                    "Transações de transferência são sempre pagas e não podem ficar pendentes.");
        }
    }

    public void delete(Long currentUserId, Long transactionId) {
        Transaction existing = findOwnedOrThrow(currentUserId, transactionId);
        if (existing.transferId() != null) {
            throw new TransactionLinkedToTransferException(
                    "Esta transação faz parte de uma transferência. Exclua a transferência inteira na tela de Transferências.");
        }
        transactionRepositoryPort.deleteById(transactionId);
    }

    private Transaction findOwnedOrThrow(Long currentUserId, Long transactionId) {
        Transaction transaction =
                transactionRepositoryPort
                        .findById(transactionId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Transação não encontrada: " + transactionId));
        requireOwnedAccount(currentUserId, transaction.accountId());
        return transaction;
    }

    private void requireOwnedAccount(Long currentUserId, Long accountId) {
        accountRepositoryPort
                .findById(accountId)
                .filter(account -> account.belongsTo(currentUserId))
                .orElseThrow(
                        () -> new ResourceNotFoundException("Conta não encontrada: " + accountId));
    }

    private void requireMatchingCategoryTypeIfPresent(
            Long currentUserId, Long categoryId, CategoryType type) {
        if (categoryId == null) {
            return;
        }
        Category category =
                categoryRepositoryPort
                        .findById(categoryId)
                        .filter(candidate -> candidate.isVisibleTo(currentUserId))
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Categoria não encontrada: " + categoryId));
        if (category.type() != type) {
            throw new CategoryTypeMismatchException(
                    "A categoria \""
                            + category.name()
                            + "\" é do tipo "
                            + category.type()
                            + " e não pode ser usada em uma transação do tipo "
                            + type
                            + ".");
        }
    }

    private void requireOwnedClientIfPresent(Long currentUserId, Long clientId) {
        if (clientId == null) {
            return;
        }
        clientRepositoryPort
                .findById(clientId)
                .filter(client -> client.belongsTo(currentUserId))
                .orElseThrow(
                        () -> new ResourceNotFoundException("Cliente não encontrado: " + clientId));
    }
}
