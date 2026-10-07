package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.model.Debt;
import com.lmf.finpro.domain.port.out.DebtRepositoryPort;
import com.lmf.finpro.infrastructure.persistence.entity.DebtJpaEntity;
import com.lmf.finpro.infrastructure.persistence.repository.DebtJpaRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DebtRepositoryAdapter implements DebtRepositoryPort {

    private final DebtJpaRepository repository;

    @Override
    public Debt save(Debt debt) {
        return toDomain(
                repository.save(
                        DebtJpaEntity.builder()
                                .id(debt.id())
                                .householdId(debt.householdId())
                                .name(debt.name())
                                .type(debt.type())
                                .creditor(debt.creditor())
                                .createdAt(debt.createdAt())
                                .build()));
    }

    @Override
    public Optional<Debt> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Debt> findAllByHouseholdId(Long householdId) {
        return repository.findByHouseholdIdOrderByNameAsc(householdId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private Debt toDomain(DebtJpaEntity entity) {
        return new Debt(
                entity.getId(),
                entity.getHouseholdId(),
                entity.getName(),
                entity.getType(),
                entity.getCreditor(),
                entity.getCreatedAt());
    }
}
