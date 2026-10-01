package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.PageQuery;
import com.lmf.finpro.domain.model.PageResult;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.TransactionSearchCriteria;
import com.lmf.finpro.domain.model.TransactionSortOrder;
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

    /**
     * Transações de um cliente, de um tipo, dentro de [start, end), ordenadas por data crescente.
     */
    List<Transaction> findAllByClientIdAndTypeAndDateBetween(
            Long clientId, CategoryType type, LocalDate start, LocalDate end);

    /** Soma das transações já pagas (status PAID) de uma conta, de um tipo. */
    BigDecimal sumPaidAmountByAccountIdAndType(Long accountId, CategoryType type);

    /**
     * Soma de transações (sem transferências) do usuário numa categoria, dentro de um intervalo de
     * datas [start, end).
     */
    BigDecimal sumAmountByUserIdAndCategoryIdAndTypeBetween(
            Long userId, Long categoryId, CategoryType type, LocalDate start, LocalDate end);

    /**
     * Igual a {@link #sumAmountByUserIdAndCategoryIdAndTypeBetween}, restrito às transações
     * vinculadas ao cliente informado.
     */
    BigDecimal sumAmountByUserIdAndCategoryIdAndClientIdAndTypeBetween(
            Long userId,
            Long categoryId,
            Long clientId,
            CategoryType type,
            LocalDate start,
            LocalDate end);

    boolean existsByAccountId(Long accountId);

    boolean existsByCategoryId(Long categoryId);

    boolean existsByClientId(Long clientId);

    /**
     * Transações do usuário que atendem aos filtros informados, ordenadas por data. Filtros nulos
     * não entram na consulta.
     */
    List<Transaction> search(TransactionSearchCriteria criteria);

    /** Uma página das transações que atendem aos filtros, na ordem pedida. */
    PageResult<Transaction> searchPage(
            TransactionSearchCriteria criteria, PageQuery pageQuery, TransactionSortOrder order);

    /**
     * Soma do valor em reais das transações do usuário de um tipo dentro de [start, end), sem as
     * transferências entre contas próprias, de qualquer situação.
     */
    BigDecimal sumBaseAmountByUserIdAndTypeBetween(
            Long userId, CategoryType type, LocalDate start, LocalDate end);

    void deleteById(Long id);
}
