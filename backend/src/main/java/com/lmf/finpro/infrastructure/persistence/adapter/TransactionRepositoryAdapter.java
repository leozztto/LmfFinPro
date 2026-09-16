package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import com.lmf.finpro.infrastructure.persistence.entity.TransactionJpaEntity;
import com.lmf.finpro.infrastructure.persistence.mapper.TransactionPersistenceMapper;
import com.lmf.finpro.infrastructure.persistence.repository.TransactionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

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
    public void deleteById(Long id) {
        transactionJpaRepository.deleteById(id);
    }
}
