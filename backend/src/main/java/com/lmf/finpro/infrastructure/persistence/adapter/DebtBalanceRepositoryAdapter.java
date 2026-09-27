package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.model.DebtBalance;
import com.lmf.finpro.domain.port.out.DebtBalanceRepositoryPort;
import com.lmf.finpro.infrastructure.persistence.entity.DebtBalanceJpaEntity;
import com.lmf.finpro.infrastructure.persistence.repository.DebtBalanceJpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DebtBalanceRepositoryAdapter implements DebtBalanceRepositoryPort {

    private final DebtBalanceJpaRepository repository;

    @Override
    public DebtBalance save(DebtBalance balance) {
        return toDomain(
                repository.save(
                        DebtBalanceJpaEntity.builder()
                                .id(balance.id())
                                .debtId(balance.debtId())
                                .balanceDate(balance.balanceDate())
                                .balance(balance.balance())
                                .createdAt(balance.createdAt())
                                .build()));
    }

    @Override
    public Optional<DebtBalance> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<DebtBalance> findByDebtIdAndDate(Long debtId, LocalDate balanceDate) {
        return repository.findByDebtIdAndBalanceDate(debtId, balanceDate).map(this::toDomain);
    }

    @Override
    public List<DebtBalance> findAllByDebtId(Long debtId) {
        return repository.findByDebtIdOrderByBalanceDateDesc(debtId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<DebtBalance> findAllByDebtIds(List<Long> debtIds) {
        if (debtIds.isEmpty()) {
            return List.of();
        }
        return repository.findByDebtIdIn(debtIds).stream().map(this::toDomain).toList();
    }

    @Override
    public long countByDebtId(Long debtId) {
        return repository.countByDebtId(debtId);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private DebtBalance toDomain(DebtBalanceJpaEntity entity) {
        return new DebtBalance(
                entity.getId(),
                entity.getDebtId(),
                entity.getBalanceDate(),
                entity.getBalance(),
                entity.getCreatedAt());
    }
}
