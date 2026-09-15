package com.lmf.finpro.repository;

import com.lmf.finpro.domain.TaxEstimate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxEstimateRepository extends JpaRepository<TaxEstimate, Long> {
}
