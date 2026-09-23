package com.lmf.finpro.application.importbatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.exception.CategoryTypeMismatchException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.CategoryRule;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.domain.model.ClientWorkType;
import com.lmf.finpro.domain.model.DocumentType;
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
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImportApplicationServiceTest {

    @Mock private ImportBatchRepositoryPort importBatchRepositoryPort;
    @Mock private TransactionRepositoryPort transactionRepositoryPort;
    @Mock private CategoryRuleRepositoryPort categoryRuleRepositoryPort;
    @Mock private CategoryRepositoryPort categoryRepositoryPort;
    @Mock private AccountRepositoryPort accountRepositoryPort;
    @Mock private ClientRepositoryPort clientRepositoryPort;

    @InjectMocks private ImportApplicationService service;

    private static Account ownedAccount() {
        return new Account(
                1L, 10L, "Conta", AccountType.CHECKING, BigDecimal.ZERO, LocalDateTime.now());
    }

    private static InputStream csv(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void importCsvThrowsWhenAccountNotOwned() {
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.importFile(
                                        10L,
                                        1L,
                                        "extrato.csv",
                                        csv("date,description,amount\n2026-09-01,X,100\n")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void importCsvAutoCategorizesTransactionMatchingAnExistingRule() {
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(ownedAccount()));
        when(importBatchRepositoryPort.save(any()))
                .thenAnswer(
                        invocation -> {
                            ImportBatch batch = invocation.getArgument(0);
                            return batch.id() == null
                                    ? new ImportBatch(
                                            99L,
                                            batch.userId(),
                                            batch.accountId(),
                                            batch.originalFile(),
                                            batch.format(),
                                            batch.importedAt(),
                                            batch.status())
                                    : batch;
                        });
        CategoryRule rule = new CategoryRule(1L, 10L, "UBER", 5L, 3);
        when(categoryRuleRepositoryPort.findAllByUserIdOrderByWeightDesc(10L))
                .thenReturn(List.of(rule));
        Category expenseCategory =
                new Category(5L, 10L, "Transporte", CategoryType.EXPENSE, null, null);
        when(categoryRepositoryPort.findById(5L)).thenReturn(Optional.of(expenseCategory));
        when(transactionRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.importFile(
                10L,
                1L,
                "extrato.csv",
                csv("date,description,amount\n2026-09-01,UBER TRIP,-45.50\n"));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepositoryPort).save(captor.capture());
        Transaction saved = captor.getValue();
        assertThat(saved.categoryId()).isEqualTo(5L);
        assertThat(saved.type()).isEqualTo(CategoryType.EXPENSE);
        assertThat(saved.amount()).isEqualByComparingTo("45.50");
    }

    @Test
    void importCsvLeavesCategoryNullWhenNoRuleMatches() {
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(ownedAccount()));
        when(importBatchRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(categoryRuleRepositoryPort.findAllByUserIdOrderByWeightDesc(10L))
                .thenReturn(List.of());
        when(transactionRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.importFile(
                10L,
                1L,
                "extrato.csv",
                csv("date,description,amount\n2026-09-01,Desconhecido,100\n"));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepositoryPort).save(captor.capture());
        assertThat(captor.getValue().categoryId()).isNull();
    }

    @Test
    void importCsvLeavesCategoryNullWhenMatchingRuleHasADifferentCategoryType() {
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(ownedAccount()));
        when(importBatchRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        CategoryRule rule = new CategoryRule(1L, 10L, "SALARIO", 5L, 3);
        when(categoryRuleRepositoryPort.findAllByUserIdOrderByWeightDesc(10L))
                .thenReturn(List.of(rule));
        Category expenseCategory =
                new Category(5L, 10L, "Transporte", CategoryType.EXPENSE, null, null);
        when(categoryRepositoryPort.findById(5L)).thenReturn(Optional.of(expenseCategory));
        when(transactionRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.importFile(
                10L,
                1L,
                "extrato.csv",
                csv("date,description,amount\n2026-09-01,SALARIO MENSAL,3000\n"));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepositoryPort).save(captor.capture());
        assertThat(captor.getValue().categoryId()).isNull();
    }

    @Test
    void importCsvLeavesCategoryNullWhenRuleReferencesADeletedCategory() {
        when(accountRepositoryPort.findById(1L)).thenReturn(Optional.of(ownedAccount()));
        when(importBatchRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        CategoryRule rule = new CategoryRule(1L, 10L, "ALUGUEL", 5L, 3);
        when(categoryRuleRepositoryPort.findAllByUserIdOrderByWeightDesc(10L))
                .thenReturn(List.of(rule));
        when(categoryRepositoryPort.findById(5L)).thenReturn(Optional.empty());
        when(transactionRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.importFile(
                10L,
                1L,
                "extrato.csv",
                csv("date,description,amount\n2026-09-01,ALUGUEL MENSAL,-1500\n"));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepositoryPort).save(captor.capture());
        assertThat(captor.getValue().categoryId()).isNull();
    }

    @Test
    void reviewTransactionSucceedsWithoutChangingCategoryOrClient() {
        ImportBatch batch =
                new ImportBatch(
                        1L,
                        10L,
                        1L,
                        "extrato.csv",
                        ImportFormat.CSV,
                        LocalDateTime.now(),
                        ImportStatus.COMPLETED);
        when(importBatchRepositoryPort.findById(1L)).thenReturn(Optional.of(batch));
        Transaction existing =
                new Transaction(
                        7L,
                        1L,
                        null,
                        null,
                        "Desc",
                        BigDecimal.TEN,
                        LocalDate.now(),
                        CategoryType.EXPENSE,
                        com.lmf.finpro.domain.model.TransactionOrigin.IMPORTED,
                        LocalDateTime.now(),
                        null,
                        1L);
        when(transactionRepositoryPort.findById(7L)).thenReturn(Optional.of(existing));
        when(transactionRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.reviewTransaction(10L, 1L, 7L, null, null);

        verify(categoryRuleRepositoryPort, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void reviewTransactionThrowsWhenCategoryDoesNotExist() {
        ImportBatch batch =
                new ImportBatch(
                        1L,
                        10L,
                        1L,
                        "extrato.csv",
                        ImportFormat.CSV,
                        LocalDateTime.now(),
                        ImportStatus.COMPLETED);
        when(importBatchRepositoryPort.findById(1L)).thenReturn(Optional.of(batch));
        Transaction existing =
                new Transaction(
                        7L,
                        1L,
                        null,
                        null,
                        "Desc",
                        BigDecimal.TEN,
                        LocalDate.now(),
                        CategoryType.EXPENSE,
                        com.lmf.finpro.domain.model.TransactionOrigin.IMPORTED,
                        LocalDateTime.now(),
                        null,
                        1L);
        when(transactionRepositoryPort.findById(7L)).thenReturn(Optional.of(existing));
        when(categoryRepositoryPort.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reviewTransaction(10L, 1L, 7L, 999L, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void reviewTransactionThrowsWhenClientDoesNotBelongToCurrentUser() {
        ImportBatch batch =
                new ImportBatch(
                        1L,
                        10L,
                        1L,
                        "extrato.csv",
                        ImportFormat.CSV,
                        LocalDateTime.now(),
                        ImportStatus.COMPLETED);
        when(importBatchRepositoryPort.findById(1L)).thenReturn(Optional.of(batch));
        Transaction existing =
                new Transaction(
                        7L,
                        1L,
                        null,
                        null,
                        "Desc",
                        BigDecimal.TEN,
                        LocalDate.now(),
                        CategoryType.EXPENSE,
                        com.lmf.finpro.domain.model.TransactionOrigin.IMPORTED,
                        LocalDateTime.now(),
                        null,
                        1L);
        when(transactionRepositoryPort.findById(7L)).thenReturn(Optional.of(existing));
        Client otherUsersClient =
                new Client(
                        3L,
                        999L,
                        "Outro",
                        null,
                        null,
                        DocumentType.CPF,
                        null,
                        ClientWorkType.PJ,
                        null,
                        null,
                        true);
        when(clientRepositoryPort.findById(3L)).thenReturn(Optional.of(otherUsersClient));

        assertThatThrownBy(() -> service.reviewTransaction(10L, 1L, 7L, null, 3L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void reviewTransactionSucceedsWhenClientBelongsToCurrentUser() {
        ImportBatch batch =
                new ImportBatch(
                        1L,
                        10L,
                        1L,
                        "extrato.csv",
                        ImportFormat.CSV,
                        LocalDateTime.now(),
                        ImportStatus.COMPLETED);
        when(importBatchRepositoryPort.findById(1L)).thenReturn(Optional.of(batch));
        Transaction existing =
                new Transaction(
                        7L,
                        1L,
                        null,
                        null,
                        "Desc",
                        BigDecimal.TEN,
                        LocalDate.now(),
                        CategoryType.EXPENSE,
                        com.lmf.finpro.domain.model.TransactionOrigin.IMPORTED,
                        LocalDateTime.now(),
                        null,
                        1L);
        when(transactionRepositoryPort.findById(7L)).thenReturn(Optional.of(existing));
        Client ownedClient =
                new Client(
                        3L,
                        10L,
                        "Cliente",
                        null,
                        null,
                        DocumentType.CPF,
                        null,
                        ClientWorkType.PJ,
                        null,
                        null,
                        true);
        when(clientRepositoryPort.findById(3L)).thenReturn(Optional.of(ownedClient));
        when(transactionRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = service.reviewTransaction(10L, 1L, 7L, null, 3L);

        assertThat(result.clientId()).isEqualTo(3L);
    }

    @Test
    void reviewTransactionReinforcesExistingRuleWhenSameCategoryConfirmedAgain() {
        ImportBatch batch =
                new ImportBatch(
                        1L,
                        10L,
                        1L,
                        "extrato.csv",
                        ImportFormat.CSV,
                        LocalDateTime.now(),
                        ImportStatus.COMPLETED);
        when(importBatchRepositoryPort.findById(1L)).thenReturn(Optional.of(batch));
        Transaction existing =
                Transaction.createImported(
                        1L,
                        null,
                        "UBER TRIP",
                        BigDecimal.valueOf(45),
                        LocalDate.now(),
                        CategoryType.EXPENSE,
                        1L);
        Transaction existingWithId =
                new Transaction(
                        7L,
                        existing.accountId(),
                        null,
                        null,
                        existing.description(),
                        existing.amount(),
                        existing.transactionDate(),
                        existing.type(),
                        existing.origin(),
                        existing.createdAt(),
                        null,
                        1L);
        when(transactionRepositoryPort.findById(7L)).thenReturn(Optional.of(existingWithId));
        Category category = new Category(5L, 10L, "Transporte", CategoryType.EXPENSE, null, null);
        when(categoryRepositoryPort.findById(5L)).thenReturn(Optional.of(category));
        when(transactionRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        CategoryRule existingRule = new CategoryRule(2L, 10L, "UBER TRIP", 5L, 3);
        when(categoryRuleRepositoryPort.findByUserIdAndPattern(10L, "UBER TRIP"))
                .thenReturn(Optional.of(existingRule));
        when(categoryRuleRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.reviewTransaction(10L, 1L, 7L, 5L, null);

        ArgumentCaptor<CategoryRule> ruleCaptor = ArgumentCaptor.forClass(CategoryRule.class);
        verify(categoryRuleRepositoryPort).save(ruleCaptor.capture());
        assertThat(ruleCaptor.getValue().weight()).isEqualTo(4);
    }

    @Test
    void reviewTransactionThrowsWhenCategoryTypeMismatchesTransactionType() {
        ImportBatch batch =
                new ImportBatch(
                        1L,
                        10L,
                        1L,
                        "extrato.csv",
                        ImportFormat.CSV,
                        LocalDateTime.now(),
                        ImportStatus.COMPLETED);
        when(importBatchRepositoryPort.findById(1L)).thenReturn(Optional.of(batch));
        Transaction existing =
                new Transaction(
                        7L,
                        1L,
                        null,
                        null,
                        "Desc",
                        BigDecimal.TEN,
                        LocalDate.now(),
                        CategoryType.EXPENSE,
                        com.lmf.finpro.domain.model.TransactionOrigin.IMPORTED,
                        LocalDateTime.now(),
                        null,
                        1L);
        when(transactionRepositoryPort.findById(7L)).thenReturn(Optional.of(existing));
        Category incomeCategory = new Category(5L, 10L, "Salário", CategoryType.INCOME, null, null);
        when(categoryRepositoryPort.findById(5L)).thenReturn(Optional.of(incomeCategory));

        assertThatThrownBy(() -> service.reviewTransaction(10L, 1L, 7L, 5L, null))
                .isInstanceOf(CategoryTypeMismatchException.class);
    }

    @Test
    void reviewTransactionThrowsWhenTransactionDoesNotBelongToBatch() {
        ImportBatch batch =
                new ImportBatch(
                        1L,
                        10L,
                        1L,
                        "extrato.csv",
                        ImportFormat.CSV,
                        LocalDateTime.now(),
                        ImportStatus.COMPLETED);
        when(importBatchRepositoryPort.findById(1L)).thenReturn(Optional.of(batch));
        Transaction fromAnotherBatch =
                new Transaction(
                        7L,
                        1L,
                        null,
                        null,
                        "Desc",
                        BigDecimal.TEN,
                        LocalDate.now(),
                        CategoryType.EXPENSE,
                        com.lmf.finpro.domain.model.TransactionOrigin.IMPORTED,
                        LocalDateTime.now(),
                        null,
                        999L);
        when(transactionRepositoryPort.findById(7L)).thenReturn(Optional.of(fromAnotherBatch));

        assertThatThrownBy(() -> service.reviewTransaction(10L, 1L, 7L, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listTransactionsThrowsWhenBatchNotOwned() {
        when(importBatchRepositoryPort.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listTransactions(10L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
