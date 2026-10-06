package com.lmf.finpro.application.importbatch;

import com.lmf.finpro.application.FlowLog;
import com.lmf.finpro.application.exchangerate.ExchangeRateApplicationService;
import com.lmf.finpro.domain.exception.CategoryTypeMismatchException;
import com.lmf.finpro.domain.exception.ImportFileInvalidException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Account;
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
import com.lmf.finpro.domain.port.out.RecordAuthorshipPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportApplicationService {

    private final ImportBatchRepositoryPort importBatchRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final CategoryRuleRepositoryPort categoryRuleRepositoryPort;
    private final CategoryRepositoryPort categoryRepositoryPort;
    private final AccountRepositoryPort accountRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;
    private final ExchangeRateApplicationService exchangeRateApplicationService;
    private final RecordAuthorshipPort recordAuthorshipPort;

    /** Sem autor conhecido (uso do sistema): qualquer membro do grupo pode excluir depois. */
    public ImportBatch importFile(
            Long currentHouseholdId,
            Long accountId,
            String originalFileName,
            InputStream fileContent) {
        return importFile(currentHouseholdId, null, accountId, originalFileName, fileContent);
    }

    /**
     * Quem importou fica como autor dos lançamentos: numa conta compartilhada, só ele os exclui.
     */
    public ImportBatch importFile(
            Long currentHouseholdId,
            Long currentUserId,
            Long accountId,
            String originalFileName,
            InputStream fileContent) {
        log.debug(
                "Importando arquivo na conta={} para o usuário={}", accountId, currentHouseholdId);
        Account account = requireOwnedAccount(currentHouseholdId, accountId);
        ImportFormat format = detectFormat(originalFileName);
        List<ParsedTransactionRow> rows =
                switch (format) {
                    case CSV -> CsvTransactionParser.parse(fileContent);
                    case OFX -> OfxTransactionParser.parse(fileContent);
                };

        ImportBatch batch =
                importBatchRepositoryPort.save(
                        ImportBatch.start(currentHouseholdId, accountId, originalFileName, format));

        List<CategoryRule> rules =
                categoryRuleRepositoryPort.findVisibleToUserOrderByPriorityDesc(currentHouseholdId);
        Set<DuplicateKey> existingKeys = duplicateKeysOf(accountId);
        int duplicateCount = 0;
        int uncategorizedCount = 0;
        List<Long> importedIds = new java.util.ArrayList<>();
        for (ParsedTransactionRow row : rows) {
            CategoryType type =
                    row.signedAmount().signum() < 0 ? CategoryType.EXPENSE : CategoryType.INCOME;
            BigDecimal amount = row.signedAmount().abs();
            DuplicateKey key =
                    new DuplicateKey(
                            row.date(), row.time(), row.description().trim(), amount, type);
            if (!existingKeys.add(key)) {
                duplicateCount++;
                continue;
            }
            Long categoryId = matchCategory(rules, row.description(), type);
            if (categoryId == null) {
                uncategorizedCount++;
            }
            Transaction imported =
                    Transaction.createImported(
                            accountId,
                            categoryId,
                            row.description(),
                            amount,
                            row.date(),
                            row.time(),
                            type,
                            batch.id());
            if (!account.currency().isBase()) {
                imported =
                        imported.withBaseAmount(
                                exchangeRateApplicationService.toBrl(
                                        account.currency(), imported.amount(), row.date()));
            }
            importedIds.add(transactionRepositoryPort.save(imported).id());
        }
        if (currentUserId != null) {
            recordAuthorshipPort.recordTransactionAuthors(importedIds, currentUserId);
        }

        FlowLog.detail("batchId", batch.id());
        FlowLog.detail("format", format);
        FlowLog.detail("rows", rows.size());
        FlowLog.detail("imported", rows.size() - duplicateCount);
        FlowLog.detail("duplicates", duplicateCount);
        FlowLog.detail("uncategorized", uncategorizedCount);
        log.info(
                "Importação={} concluída formato={} linhas={} importadas={} duplicadas={}"
                        + " semCategoria={}",
                batch.id(),
                format,
                rows.size(),
                rows.size() - duplicateCount,
                duplicateCount,
                uncategorizedCount);
        return importBatchRepositoryPort.save(
                batch.withStatus(ImportStatus.COMPLETED).withDuplicateCount(duplicateCount));
    }

    /**
     * Chave (data, hora, descrição, valor, tipo) de cada transação já lançada na conta — usada para
     * pular, sem duplicar, uma linha do arquivo que já existe (de uma importação anterior ou de uma
     * linha repetida dentro do próprio arquivo sendo importado agora).
     */
    private Set<DuplicateKey> duplicateKeysOf(Long accountId) {
        Set<DuplicateKey> keys = new HashSet<>();
        for (Transaction transaction :
                transactionRepositoryPort.findAllByAccountIds(List.of(accountId))) {
            keys.add(
                    new DuplicateKey(
                            transaction.transactionDate(),
                            transaction.transactionTime(),
                            transaction.description().trim(),
                            transaction.amount(),
                            transaction.type()));
        }
        return keys;
    }

    private record DuplicateKey(
            LocalDate date,
            LocalTime time,
            String description,
            BigDecimal amount,
            CategoryType type) {}

    public List<ImportBatch> list(Long currentHouseholdId) {
        log.debug("Listando importações do usuário={}", currentHouseholdId);
        return importBatchRepositoryPort.findAllByHouseholdId(currentHouseholdId);
    }

    public ImportBatch getById(Long currentHouseholdId, Long batchId) {
        log.debug("Buscando importação={} do usuário={}", batchId, currentHouseholdId);
        return findOwnedBatchOrThrow(currentHouseholdId, batchId);
    }

    public List<Transaction> listTransactions(Long currentHouseholdId, Long batchId) {
        log.debug(
                "Listando transações da importação={} do usuário={}", batchId, currentHouseholdId);
        ImportBatch batch = findOwnedBatchOrThrow(currentHouseholdId, batchId);
        return transactionRepositoryPort.findAllByImportBatchId(batch.id());
    }

    /**
     * Corrige a categoria/cliente de uma transação importada. Quando uma categoria é informada, a
     * regra usada para essa descrição é reforçada (ou criada) — é assim que o motor "aprende" com
     * as correções do usuário para futuras importações.
     */
    public Transaction reviewTransaction(
            Long currentHouseholdId,
            Long batchId,
            Long transactionId,
            Long categoryId,
            Long clientId) {
        log.debug(
                "Revisando transação={} da importação={} do usuário={}",
                transactionId,
                batchId,
                currentHouseholdId);
        ImportBatch batch = findOwnedBatchOrThrow(currentHouseholdId, batchId);
        Transaction existing =
                transactionRepositoryPort
                        .findById(transactionId)
                        .filter(transaction -> batch.id().equals(transaction.importBatchId()))
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Transação não encontrada nesta importação: "
                                                        + transactionId));

        requireMatchingCategoryTypeIfPresent(currentHouseholdId, categoryId, existing.type());
        requireOwnedClientIfPresent(currentHouseholdId, clientId);

        Transaction updated =
                transactionRepositoryPort.save(
                        existing.withDetails(
                                        categoryId,
                                        clientId,
                                        existing.description(),
                                        existing.amount(),
                                        existing.transactionDate(),
                                        existing.type())
                                .withBaseAmount(existing.baseAmount()));

        if (categoryId != null) {
            reinforceRule(currentHouseholdId, existing.description(), categoryId);
        }

        return updated;
    }

    private ImportFormat detectFormat(String originalFileName) {
        String lowerFileName =
                originalFileName == null ? "" : originalFileName.toLowerCase(Locale.ROOT);
        if (lowerFileName.endsWith(".csv")) {
            return ImportFormat.CSV;
        }
        if (lowerFileName.endsWith(".ofx")) {
            return ImportFormat.OFX;
        }
        throw new ImportFileInvalidException(
                "Formato de arquivo não suportado. Envie um arquivo .csv ou .ofx.");
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

    private void reinforceRule(Long householdId, String description, Long categoryId) {
        String pattern = description.trim();
        CategoryRule rule =
                categoryRuleRepositoryPort
                        .findByHouseholdIdAndPattern(householdId, pattern)
                        .map(existing -> existing.reinforcedWith(categoryId))
                        .orElseGet(() -> CategoryRule.create(householdId, pattern, categoryId));
        categoryRuleRepositoryPort.save(rule);
    }

    private ImportBatch findOwnedBatchOrThrow(Long currentHouseholdId, Long batchId) {
        return importBatchRepositoryPort
                .findById(batchId)
                .filter(batch -> batch.belongsTo(currentHouseholdId))
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Importação não encontrada: " + batchId));
    }

    private Account requireOwnedAccount(Long currentHouseholdId, Long accountId) {
        return accountRepositoryPort
                .findById(accountId)
                .filter(account -> account.belongsTo(currentHouseholdId))
                .orElseThrow(
                        () -> new ResourceNotFoundException("Conta não encontrada: " + accountId));
    }

    private void requireMatchingCategoryTypeIfPresent(
            Long currentHouseholdId, Long categoryId, CategoryType type) {
        if (categoryId == null) {
            return;
        }
        Category category =
                categoryRepositoryPort
                        .findById(categoryId)
                        .filter(candidate -> candidate.isVisibleTo(currentHouseholdId))
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

    private void requireOwnedClientIfPresent(Long currentHouseholdId, Long clientId) {
        if (clientId == null) {
            return;
        }
        clientRepositoryPort
                .findById(clientId)
                .filter(client -> client.belongsTo(currentHouseholdId))
                .orElseThrow(
                        () -> new ResourceNotFoundException("Cliente não encontrado: " + clientId));
    }
}
