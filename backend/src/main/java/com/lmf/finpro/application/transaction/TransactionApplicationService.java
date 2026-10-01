package com.lmf.finpro.application.transaction;

import com.lmf.finpro.application.attachment.TransactionAttachmentApplicationService;
import com.lmf.finpro.application.exchangerate.ExchangeRateApplicationService;
import com.lmf.finpro.application.tag.TagApplicationService;
import com.lmf.finpro.domain.exception.CategoryTypeMismatchException;
import com.lmf.finpro.domain.exception.InvalidTagException;
import com.lmf.finpro.domain.exception.PaidTransactionLockedException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.exception.TransactionLinkedToTransferException;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Currency;
import com.lmf.finpro.domain.model.PageQuery;
import com.lmf.finpro.domain.model.PageResult;
import com.lmf.finpro.domain.model.Tag;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.TransactionSearchCriteria;
import com.lmf.finpro.domain.model.TransactionSortOrder;
import com.lmf.finpro.domain.model.TransactionStatus;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionApplicationService {

    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 100;

    private final TransactionRepositoryPort transactionRepositoryPort;
    private final AccountRepositoryPort accountRepositoryPort;
    private final CategoryRepositoryPort categoryRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;
    private final Clock clock;
    private final TransactionAttachmentApplicationService transactionAttachmentApplicationService;
    private final TagApplicationService tagApplicationService;
    private final ExchangeRateApplicationService exchangeRateApplicationService;

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
                tagNames,
                null,
                null);
    }

    /**
     * {@code amount} é o valor na moeda da conta. Operação feita em outra moeda (ex.: compra em
     * dólar no cartão em reais) informa também {@code originalCurrency} e {@code originalAmount};
     * se a moeda for a própria da conta, os dois são ignorados.
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
            List<String> tagNames,
            Currency originalCurrency,
            BigDecimal originalAmount) {
        Account account = requireOwnedAccount(currentUserId, accountId);
        requireMatchingCategoryTypeIfPresent(currentUserId, categoryId, type);
        requireOwnedClientIfPresent(currentUserId, clientId);
        TransactionStatus resolvedStatus =
                status != null
                        ? status
                        : TransactionStatus.defaultFor(transactionDate, LocalDate.now(clock));
        Transaction saved =
                transactionRepositoryPort.save(
                        withCurrencies(
                                Transaction.create(
                                        accountId,
                                        categoryId,
                                        clientId,
                                        description,
                                        amount,
                                        transactionDate,
                                        type,
                                        resolvedStatus),
                                account,
                                originalCurrency,
                                originalAmount));
        tagApplicationService.replaceTransactionTags(currentUserId, saved.id(), tagNames);
        return saved;
    }

    /**
     * Uma página das transações do usuário, da mais recente para a mais antiga, já filtrada no
     * banco. {@code page} começa em 0; {@code size} fica entre 1 e {@link #MAX_PAGE_SIZE}. Nome de
     * tag que o usuário não tem não casa com nada; se nenhuma das tags pedidas existe, a página vem
     * vazia.
     */
    public PageResult<Transaction> list(
            Long currentUserId, TransactionListFilters filters, Integer page, Integer size) {
        PageQuery pageQuery =
                new PageQuery(
                        page == null ? 0 : Math.max(page, 0),
                        size == null
                                ? DEFAULT_PAGE_SIZE
                                : Math.min(Math.max(size, 1), MAX_PAGE_SIZE));

        List<Long> tagIds = List.of();
        if (!filters.tagNames().isEmpty()) {
            Set<String> wanted =
                    filters.tagNames().stream()
                            .map(TransactionApplicationService::normalizedTagNameOrNull)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());
            tagIds =
                    tagApplicationService.tagsById(currentUserId).values().stream()
                            .filter(tag -> wanted.contains(tag.name()))
                            .map(Tag::id)
                            .toList();
            if (tagIds.isEmpty()) {
                return PageResult.empty(pageQuery.page(), pageQuery.size());
            }
        }

        return transactionRepositoryPort.searchPage(
                new TransactionSearchCriteria(
                        currentUserId,
                        filters.type(),
                        filters.startDate(),
                        filters.endDate(),
                        filters.accountId(),
                        null,
                        filters.categoryId(),
                        filters.clientId(),
                        filters.status(),
                        null,
                        null,
                        null,
                        false,
                        tagIds,
                        filters.hasAttachment()),
                pageQuery,
                TransactionSortOrder.NEWEST_FIRST);
    }

    /** Nome de tag inválido num filtro só não casa com nada, em vez de virar erro. */
    private static String normalizedTagNameOrNull(String rawName) {
        try {
            return Tag.normalizeName(rawName);
        } catch (InvalidTagException e) {
            return null;
        }
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
            TransactionStatus status,
            List<String> tagNames) {
        return update(
                currentUserId,
                transactionId,
                categoryId,
                clientId,
                description,
                amount,
                transactionDate,
                type,
                status,
                tagNames,
                null,
                null);
    }

    /**
     * {@code tagNames} nulo mantém as tags atuais. A moeda original segue a regra da criação: sem
     * ela, a operação passa a ser na moeda da conta. O valor em reais é recalculado.
     */
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
            List<String> tagNames,
            Currency originalCurrency,
            BigDecimal originalAmount) {
        Transaction existing = findOwnedOrThrow(currentUserId, transactionId);
        Account account = requireOwnedAccount(currentUserId, existing.accountId());
        requireMatchingCategoryTypeIfPresent(currentUserId, categoryId, type);
        requireOwnedClientIfPresent(currentUserId, clientId);
        Transaction updated =
                withCurrencies(
                        existing.withDetails(
                                categoryId, clientId, description, amount, transactionDate, type),
                        account,
                        originalCurrency,
                        originalAmount);
        // Perna de transferência: o valor em reais é o combinado entre as duas pontas (para elas
        // se anularem no consolidado); só muda se o valor ou a data mudarem.
        if (existing.transferId() != null
                && existing.amount().compareTo(amount) == 0
                && existing.transactionDate().isEqual(transactionDate)) {
            updated = updated.withBaseAmount(existing.baseAmount());
        }
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

    /**
     * Moeda original (só quando diferente da moeda da conta) e valor em reais, pela cotação do dia
     * da transação nas contas em outra moeda.
     */
    private Transaction withCurrencies(
            Transaction transaction,
            Account account,
            Currency originalCurrency,
            BigDecimal originalAmount) {
        boolean foreign = originalCurrency != null && originalCurrency != account.currency();
        if (foreign && (originalAmount == null || originalAmount.signum() <= 0)) {
            throw new IllegalArgumentException(
                    "Informe o valor da operação em " + originalCurrency + ".");
        }
        return transaction
                .withOriginal(foreign ? originalCurrency : null, foreign ? originalAmount : null)
                .withBaseAmount(
                        account.currency().isBase()
                                ? transaction.amount()
                                : exchangeRateApplicationService.toBrl(
                                        account.currency(),
                                        transaction.amount(),
                                        transaction.transactionDate()));
    }

    private Account requireOwnedAccount(Long currentUserId, Long accountId) {
        return accountRepositoryPort
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
