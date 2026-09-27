package com.lmf.finpro.application.transaction;

import com.lmf.finpro.application.attachment.TransactionAttachmentApplicationService;
import com.lmf.finpro.application.tag.TagApplicationService;
import com.lmf.finpro.domain.exception.CategoryTypeMismatchException;
import com.lmf.finpro.domain.exception.PaidTransactionLockedException;
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
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionApplicationService {

    private final TransactionRepositoryPort transactionRepositoryPort;
    private final AccountRepositoryPort accountRepositoryPort;
    private final CategoryRepositoryPort categoryRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;
    private final Clock clock;
    private final TransactionAttachmentApplicationService transactionAttachmentApplicationService;
    private final TagApplicationService tagApplicationService;

    /** Sem tags. */
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
        return create(
                currentUserId,
                accountId,
                categoryId,
                clientId,
                description,
                amount,
                transactionDate,
                type,
                status,
                List.of());
    }

    /**
     * {@code status} nulo usa o padrão pela data: data futura fica pendente, o resto já nasce pago.
     * As tags (por nome) são criadas se ainda não existirem, na mesma transação de banco — uma tag
     * inválida desfaz o lançamento inteiro.
     */
    @Transactional
    public Transaction create(
            Long currentUserId,
            Long accountId,
            Long categoryId,
            Long clientId,
            String description,
            BigDecimal amount,
            LocalDate transactionDate,
            CategoryType type,
            TransactionStatus status,
            List<String> tagNames) {
        requireOwnedAccount(currentUserId, accountId);
        requireMatchingCategoryTypeIfPresent(currentUserId, categoryId, type);
        requireOwnedClientIfPresent(currentUserId, clientId);
        TransactionStatus resolvedStatus =
                status != null
                        ? status
                        : TransactionStatus.defaultFor(transactionDate, LocalDate.now(clock));
        Transaction saved =
                transactionRepositoryPort.save(
                        Transaction.create(
                                accountId,
                                categoryId,
                                clientId,
                                description,
                                amount,
                                transactionDate,
                                type,
                                resolvedStatus));
        tagApplicationService.replaceTransactionTags(currentUserId, saved.id(), tagNames);
        return saved;
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

    /** {@code tagNames} nulo mantém as tags atuais. */
    @Transactional
    public Transaction update(
            Long currentUserId,
            Long transactionId,
            Long categoryId,
            Long clientId,
            String description,
            BigDecimal amount,
            LocalDate transactionDate,
            CategoryType type,
            TransactionStatus status,
            List<String> tagNames) {
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
        Transaction saved = transactionRepositoryPort.save(updated);
        if (tagNames != null) {
            tagApplicationService.replaceTransactionTags(currentUserId, transactionId, tagNames);
        }
        return saved;
    }

    /**
     * Troca só as tags. Vale para qualquer transação — paga, pendente, importada ou de
     * transferência —, porque tag é classificação e não mexe em valor nem em saldo.
     */
    public Transaction updateTags(Long currentUserId, Long transactionId, List<String> tagNames) {
        Transaction existing = findOwnedOrThrow(currentUserId, transactionId);
        tagApplicationService.replaceTransactionTags(currentUserId, transactionId, tagNames);
        return existing;
    }

    /** Marca como paga — a ação rápida da lista de transações. Paga não volta a pendente. */
    public Transaction updateStatus(
            Long currentUserId, Long transactionId, TransactionStatus status) {
        Transaction existing = findOwnedOrThrow(currentUserId, transactionId);
        requireStatusChangeAllowed(existing, status);
        return transactionRepositoryPort.save(existing.withStatus(status));
    }

    /**
     * Regras de mudança de situação, valendo tanto para a ação rápida quanto para a edição:
     *
     * <ul>
     *   <li>transferência já movimentou o dinheiro nas duas contas: não existe transferência
     *       pendente;
     *   <li>marcar como paga é definitivo: uma transação paga não volta a pendente (o pagamento já
     *       afetou o saldo e os relatórios; se foi um engano, o caminho é excluir e lançar de
     *       novo).
     * </ul>
     */
    private void requireStatusChangeAllowed(Transaction transaction, TransactionStatus status) {
        if (status != TransactionStatus.PENDING) {
            return;
        }
        if (transaction.transferId() != null) {
            throw new TransactionLinkedToTransferException(
                    "Transações de transferência são sempre pagas e não podem ficar pendentes.");
        }
        if (transaction.isPaid()) {
            throw new PaidTransactionLockedException(
                    "Esta transação já foi marcada como paga e não pode voltar a pendente.");
        }
    }

    public void delete(Long currentUserId, Long transactionId) {
        Transaction existing = findOwnedOrThrow(currentUserId, transactionId);
        if (existing.transferId() != null) {
            throw new TransactionLinkedToTransferException(
                    "Esta transação faz parte de uma transferência. Exclua a transferência inteira na tela de Transferências.");
        }
        // Os registros dos anexos saem em cascata no banco; os arquivos, só apagando do disco.
        List<String> attachmentKeys =
                transactionAttachmentApplicationService.storageKeysOf(List.of(transactionId));
        transactionRepositoryPort.deleteById(transactionId);
        transactionAttachmentApplicationService.deleteStoredFiles(attachmentKeys);
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
