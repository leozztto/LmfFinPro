package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.TaxEstimate;

import java.util.List;
import java.util.Optional;

public interface TaxEstimateRepositoryPort {
    TaxEstimate save(TaxEstimate taxEstimate);
    Optional<TaxEstimate> findById(Long id);
    List<TaxEstimate> findAllByUserId(Long userId);
    void deleteById(Long id);
}
