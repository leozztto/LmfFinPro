package com.lmf.finpro.application.recurringtransaction;

import com.lmf.finpro.domain.exception.CategoryTypeMismatchException;
import com.lmf.finpro.domain.exception.InvalidRecurrencePeriodException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.RecurrenceFrequency;
import com.lmf.finpro.domain.model.RecurringTransaction;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.domain.port.out.RecurringTransactionRepositoryPort;
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
public class RecurringTransactionApplicationService {

    private final RecurringTransactionRepositoryPort recurringTransactionRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final AccountRepositoryPort accountRepositoryPort;
    private final CategoryRepositoryPort categoryRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;
    private final Clock clock;

    /**
     * Cria a recorrência e já lança as ocorrências vencidas até hoje — uma recorrência com data
     * inicial no passado (ex.: aluguel desde janeiro) gera na hora as transações que faltam.
     */
    @Transactional
    public RecurringTransaction create(
            Long currentUserId,
            Long accountId,
            Long categoryId,
            Long clientId,
            String description,
            BigDecimal amount,
            CategoryType type,
            RecurrenceFrequency frequency,
            LocalDate startDate,
            LocalDate endDate) {
        requireOwnedAccount(currentUserId, accountId);
        requireMatchingCategoryTypeIfPresent(currentUserId, categoryId, type);
        requireOwnedClientIfPresent(currentUserId, clientId);
        requireValidPeriod(startDate, endDate);
        RecurringTransaction saved =
                recurringTransactionRepositoryPort.save(
                        RecurringTransaction.create(
                                currentUserId,
                                accountId,
                                categoryId,
                                clientId,
                                description,
                                amount,
                                type,
                                frequency,
                                startDate,
                                endDate));
        return generateDueOccurrences(saved);
    }

    public List<RecurringTransaction> list(Long currentUserId) {
        return recurringTransactionRepositoryPort.findAllByUserId(currentUserId);
    }

    @Transactional
    public RecurringTransaction update(
            Long currentUserId,
            Long recurringTransactionId,
            Long categoryId,
            Long clientId,
            String description,
            BigDecimal amount,
            LocalDate endDate,
            boolean active) {
        RecurringTransaction existing = findOwnedOrThrow(currentUserId, recurringTransactionId);
        requireMatchingCategoryTypeIfPresent(currentUserId, categoryId, existing.type());
        requireOwnedClientIfPresent(currentUserId, clientId);
        requireValidPeriod(existing.startDate(), endDate);
        RecurringTransaction updated =
                recurringTransactionRepositoryPort.save(
                        existing.withDetails(
                                categoryId,
                                clientId,
                                description,
                                amount,
                                endDate,
                                active,
                                LocalDate.now(clock)));
        return generateDueOccurrences(updated);
    }

    /** Exclui só o modelo: as transações já lançadas continuam, apenas sem o vínculo. */
    public void delete(Long currentUserId, Long recurringTransactionId) {
        findOwnedOrThrow(currentUserId, recurringTransactionId);
        recurringTransactionRepositoryPort.deleteById(recurringTransactionId);
    }

    public List<RecurringTransaction> findAllActive() {
        return recurringTransactionRepositoryPort.findAllActive();
    }

    /**
     * Lança uma transação para cada ocorrência vencida até hoje e avança o contador — na mesma
     * transação de banco, para uma falha no meio não deixar ocorrência lançada sem contar (que
     * seria lançada de novo na próxima execução).
     */
    @Transactional
    public RecurringTransaction generateDueOccurrences(RecurringTransaction recurrence) {
        List<LocalDate> dueDates = recurrence.dueOccurrenceDates(LocalDate.now(clock));
        if (dueDates.isEmpty()) {
            return recurrence;
        }
        for (LocalDate occurrenceDate : dueDates) {
            transactionRepositoryPort.save(
                    Transaction.createFromRecurrence(recurrence, occurrenceDate));
        }
        return recurringTransactionRepositoryPort.save(
                recurrence.withGeneratedOccurrences(
                        recurrence.generatedOccurrences() + dueDates.size()));
    }

    private void requireValidPeriod(LocalDate startDate, LocalDate endDate) {
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new InvalidRecurrencePeriodException(
                    "A data final da recorrência não pode ser anterior à data inicial.");
        }
    }

    /** Acesso a recorrência de outro usuário é tratado como inexistente (404), não como 403. */
    private RecurringTransaction findOwnedOrThrow(Long currentUserId, Long recurringTransactionId) {
        return recurringTransactionRepositoryPort
                .findById(recurringTransactionId)
                .filter(recurrence -> recurrence.belongsTo(currentUserId))
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Lançamento recorrente não encontrado: "
                                                + recurringTransactionId));
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
                            + " e não pode ser usada em um lançamento do tipo "
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
