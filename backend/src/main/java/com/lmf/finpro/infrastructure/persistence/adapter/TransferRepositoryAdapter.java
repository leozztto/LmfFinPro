package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.model.Transfer;
import com.lmf.finpro.domain.port.out.TransferRepositoryPort;
import com.lmf.finpro.infrastructure.persistence.mapper.TransferPersistenceMapper;
import com.lmf.finpro.infrastructure.persistence.repository.TransferJpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransferRepositoryAdapter implements TransferRepositoryPort {

    private final TransferJpaRepository transferJpaRepository;
    private final TransferPersistenceMapper mapper;

    @Override
    public Transfer save(Transfer transfer) {
        return mapper.toDomain(transferJpaRepository.save(mapper.toEntity(transfer)));
    }

    @Override
    public Optional<Transfer> findById(Long id) {
        return transferJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Transfer> findAllByHouseholdId(Long householdId) {
        return transferJpaRepository.findByHouseholdId(householdId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByAccountId(Long accountId) {
        return transferJpaRepository.existsByFromAccountIdOrToAccountId(accountId, accountId);
    }

    @Override
    public Set<Long> findCrossSpaceIds(Collection<Long> transferIds) {
        if (transferIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(transferJpaRepository.findCrossSpaceIds(transferIds));
    }

    @Override
    public void deleteById(Long id) {
        transferJpaRepository.deleteById(id);
    }
}
