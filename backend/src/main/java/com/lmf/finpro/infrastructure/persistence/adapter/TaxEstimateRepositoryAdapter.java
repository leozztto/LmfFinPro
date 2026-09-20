package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.model.TaxEstimate;
import com.lmf.finpro.domain.port.out.TaxEstimateRepositoryPort;
import com.lmf.finpro.infrastructure.persistence.mapper.TaxEstimatePersistenceMapper;
import com.lmf.finpro.infrastructure.persistence.repository.TaxEstimateJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TaxEstimateRepositoryAdapter implements TaxEstimateRepositoryPort {

    private final TaxEstimateJpaRepository taxEstimateJpaRepository;
    private final TaxEstimatePersistenceMapper mapper;

    @Override
    public TaxEstimate save(TaxEstimate taxEstimate) {
        return mapper.toDomain(taxEstimateJpaRepository.save(mapper.toEntity(taxEstimate)));
    }

    @Override
    public Optional<TaxEstimate> findById(Long id) {
        return taxEstimateJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<TaxEstimate> findAllByUserId(Long userId) {
        return taxEstimateJpaRepository.findByUserId(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        taxEstimateJpaRepository.deleteById(id);
    }
}
