package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.model.AccountValuation;
import com.lmf.finpro.domain.port.out.AccountValuationRepositoryPort;
import com.lmf.finpro.infrastructure.persistence.entity.AccountValuationJpaEntity;
import com.lmf.finpro.infrastructure.persistence.repository.AccountValuationJpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountValuationRepositoryAdapter implements AccountValuationRepositoryPort {

    private final AccountValuationJpaRepository repository;

    @Override
    public AccountValuation save(AccountValuation valuation) {
        return toDomain(
                repository.save(
                        AccountValuationJpaEntity.builder()
                                .id(valuation.id())
                                .accountId(valuation.accountId())
                                .valuationDate(valuation.valuationDate())
                                .value(valuation.value())
                                .createdAt(valuation.createdAt())
                                .build()));
    }

    @Override
    public Optional<AccountValuation> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<AccountValuation> findByAccountIdAndDate(
            Long accountId, LocalDate valuationDate) {
        return repository
                .findByAccountIdAndValuationDate(accountId, valuationDate)
                .map(this::toDomain);
    }

    @Override
    public List<AccountValuation> findAllByAccountId(Long accountId) {
        return repository.findByAccountIdOrderByValuationDateDesc(accountId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<AccountValuation> findAllByAccountIds(List<Long> accountIds) {
        if (accountIds.isEmpty()) {
            return List.of();
        }
        return repository.findByAccountIdIn(accountIds).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByAccountId(Long accountId) {
        return repository.existsByAccountId(accountId);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private AccountValuation toDomain(AccountValuationJpaEntity entity) {
        return new AccountValuation(
                entity.getId(),
                entity.getAccountId(),
                entity.getValuationDate(),
                entity.getValue(),
                entity.getCreatedAt());
    }
}
