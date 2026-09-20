package com.lmf.finpro.application.importbatch;

import com.lmf.finpro.domain.exception.CategoryTypeMismatchException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.CategoryRule;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.ImportBatch;
import com.lmf.finpro.domain.model.ImportFormat;
import com.lmf.finpro.domain.model.ImportStatus;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.CategoryRuleRepositoryPort;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.domain.port.out.ImportBatchRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.io.InputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ImportApplicationService {

    private final ImportBatchRepositoryPort importBatchRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final CategoryRuleRepositoryPort categoryRuleRepositoryPort;
    private final CategoryRepositoryPort categoryRepositoryPort;
    private final AccountRepositoryPort accountRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;

    public ImportBatch importCsv(
            Long currentUserId, Long accountId, String originalFileName, InputStream csvContent) {
        requireOwnedAccount(currentUserId, accountId);
        List<CsvTransactionParser.ParsedRow> rows = CsvTransactionParser.parse(csvContent);

        ImportBatch batch =
                importBatchRepositoryPort.save(
                        ImportBatch.start(
                                currentUserId, accountId, originalFileName, ImportFormat.CSV));

        List<CategoryRule> rules =
                categoryRuleRepositoryPort.findAllByUserIdOrderByWeightDesc(currentUserId);
        for (CsvTransactionParser.ParsedRow row : rows) {
            CategoryType type =
                    row.signedAmount().signum() < 0 ? CategoryType.EXPENSE : CategoryType.INCOME;
            Long categoryId = matchCategory(rules, row.description(), type);
            transactionRepositoryPort.save(
                    Transaction.createImported(
                            accountId,
                            categoryId,
                            row.description(),
                            row.signedAmount().abs(),
                            row.date(),
                            type,
                            batch.id()));
        }

        return importBatchRepositoryPort.save(batch.withStatus(ImportStatus.COMPLETED));
    }

    public List<ImportBatch> list(Long currentUserId) {
        return importBatchRepositoryPort.findAllByUserId(currentUserId);
    }

    public ImportBatch getById(Long currentUserId, Long batchId) {
        return findOwnedBatchOrThrow(currentUserId, batchId);
    }

    public List<Transaction> listTransactions(Long currentUserId, Long batchId) {
        ImportBatch batch = findOwnedBatchOrThrow(currentUserId, batchId);
        return transactionRepositoryPort.findAllByImportBatchId(batch.id());
    }

    /**
     * Corrige a categoria/cliente de uma transação importada. Quando uma categoria é informada, a
     * regra usada para essa descrição é reforçada (ou criada) — é assim que o motor "aprende" com
     * as correções do usuário para futuras importações.
     */
    public Transaction reviewTransaction(
            Long currentUserId, Long batchId, Long transactionId, Long categoryId, Long clientId) {
        ImportBatch batch = findOwnedBatchOrThrow(currentUserId, batchId);
        Transaction existing =
                transactionRepositoryPort
                        .findById(transactionId)
                        .filter(transaction -> batch.id().equals(transaction.importBatchId()))
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Transação não encontrada nesta importação: "
                                                        + transactionId));

        requireMatchingCategoryTypeIfPresent(currentUserId, categoryId, existing.type());
        requireOwnedClientIfPresent(currentUserId, clientId);

        Transaction updated =
                transactionRepositoryPort.save(
                        existing.withDetails(
                                categoryId,
                                clientId,
                                existing.description(),
                                existing.amount(),
                                existing.transactionDate(),
                                existing.type()));

        if (categoryId != null) {
            reinforceRule(currentUserId, existing.description(), categoryId);
        }

        return updated;
    }

    private Long matchCategory(
            List<CategoryRule> rules, String description, CategoryType expectedType) {
        for (CategoryRule rule : rules) {
            if (!rule.matches(description)) {
                continue;
            }
            Category category = categoryRepositoryPort.findById(rule.categoryId()).orElse(null);
            if (category != null && category.type() == expectedType) {
                return category.id();
            }
        }
        return null;
    }

    private void reinforceRule(Long userId, String description, Long categoryId) {
        String pattern = description.trim();
        CategoryRule rule =
                categoryRuleRepositoryPort
                        .findByUserIdAndPattern(userId, pattern)
                        .map(existing -> existing.reinforcedWith(categoryId))
                        .orElseGet(() -> CategoryRule.create(userId, pattern, categoryId));
        categoryRuleRepositoryPort.save(rule);
    }

    private ImportBatch findOwnedBatchOrThrow(Long currentUserId, Long batchId) {
        return importBatchRepositoryPort
                .findById(batchId)
                .filter(batch -> batch.belongsTo(currentUserId))
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Importação não encontrada: " + batchId));
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
