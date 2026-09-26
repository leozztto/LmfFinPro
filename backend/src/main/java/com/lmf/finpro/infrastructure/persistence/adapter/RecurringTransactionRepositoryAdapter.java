package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.model.RecurringTransaction;
import com.lmf.finpro.domain.port.out.RecurringTransactionRepositoryPort;
import com.lmf.finpro.infrastructure.persistence.mapper.RecurringTransactionPersistenceMapper;
import com.lmf.finpro.infrastructure.persistence.repository.RecurringTransactionJpaRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecurringTransactionRepositoryAdapter implements RecurringTransactionRepositoryPort {

    private final RecurringTransactionJpaRepository recurringTransactionJpaRepository;
    private final RecurringTransactionPersistenceMapper mapper;

    @Override
    public RecurringTransaction save(RecurringTransaction recurringTransaction) {
        return mapper.toDomain(
                recurringTransactionJpaRepository.save(mapper.toEntity(recurringTransaction)));
    }

    @Override
    public Optional<RecurringTransaction> findById(Long id) {
        return recurringTransactionJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<RecurringTransaction> findAllByUserId(Long userId) {
        return recurringTransactionJpaRepository.findByUserIdOrderByStartDateAsc(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<RecurringTransaction> findAllActive() {
        return recurringTransactionJpaRepository.findByActiveTrue().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByAccountId(Long accountId) {
        return recurringTransactionJpaRepository.existsByAccountId(accountId);
    }

    @Override
    public void deleteById(Long id) {
        recurringTransactionJpaRepository.deleteById(id);
    }
}
