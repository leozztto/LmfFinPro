package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import com.lmf.finpro.infrastructure.persistence.mapper.TransactionPersistenceMapper;
import com.lmf.finpro.infrastructure.persistence.repository.TransactionJpaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionRepositoryAdapter implements TransactionRepositoryPort {

    private final TransactionJpaRepository transactionJpaRepository;
    private final TransactionPersistenceMapper mapper;

    @Override
    public Transaction save(Transaction transaction) {
        return mapper.toDomain(transactionJpaRepository.save(mapper.toEntity(transaction)));
    }

    @Override
    public Optional<Transaction> findById(Long id) {
        return transactionJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Transaction> findAllByAccountIds(List<Long> accountIds) {
        return transactionJpaRepository.findByAccountIdIn(accountIds).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Transaction> findAllByTransferIds(List<Long> transferIds) {
        if (transferIds.isEmpty()) {
            return List.of();
        }
        return transactionJpaRepository.findByTransferIdIn(transferIds).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Transaction> findAllByImportBatchId(Long importBatchId) {
        return transactionJpaRepository.findByImportBatchId(importBatchId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public BigDecimal sumAmountByAccountIdAndType(Long accountId, CategoryType type) {
        return transactionJpaRepository.sumAmountByAccountIdAndType(accountId, type);
    }

    @Override
    public BigDecimal sumAmountByUserIdAndCategoryIdAndTypeBetween(
            Long userId, Long categoryId, CategoryType type, LocalDate start, LocalDate end) {
        return transactionJpaRepository.sumAmountByUserIdAndCategoryIdAndTypeBetween(
                userId, categoryId, type, start, end);
    }

    @Override
    public boolean existsByAccountId(Long accountId) {
        return transactionJpaRepository.existsByAccountId(accountId);
    }

    @Override
    public boolean existsByCategoryId(Long categoryId) {
        return transactionJpaRepository.existsByCategoryId(categoryId);
    }

    @Override
    public boolean existsByClientId(Long clientId) {
        return transactionJpaRepository.existsByClientId(clientId);
    }

    @Override
    public void deleteById(Long id) {
        transactionJpaRepository.deleteById(id);
    }
}
