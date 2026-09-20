package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepositoryPort {
    Transaction save(Transaction transaction);
    Optional<Transaction> findById(Long id);
    List<Transaction> findAllByAccountIds(List<Long> accountIds);
    List<Transaction> findAllByTransferIds(List<Long> transferIds);
    List<Transaction> findAllByImportBatchId(Long importBatchId);
    BigDecimal sumAmountByAccountIdAndType(Long accountId, CategoryType type);

    /** Soma de transações (sem transferências) do usuário numa categoria, dentro de um intervalo de datas [start, end). */
    BigDecimal sumAmountByUserIdAndCategoryIdAndTypeBetween(
        Long userId, Long categoryId, CategoryType type, LocalDate start, LocalDate end
    );
    boolean existsByAccountId(Long accountId);
    boolean existsByCategoryId(Long categoryId);
    boolean existsByClientId(Long clientId);
    void deleteById(Long id);
}
