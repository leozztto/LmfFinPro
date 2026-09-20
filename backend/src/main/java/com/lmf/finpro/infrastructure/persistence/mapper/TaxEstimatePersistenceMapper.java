package com.lmf.finpro.infrastructure.persistence.mapper;

import com.lmf.finpro.domain.model.TaxEstimate;
import com.lmf.finpro.infrastructure.persistence.entity.TaxEstimateJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Component
public class TaxEstimatePersistenceMapper {

    public TaxEstimateJpaEntity toEntity(TaxEstimate taxEstimate) {
        return TaxEstimateJpaEntity.builder()
            .id(taxEstimate.id())
            .user(UserJpaEntity.builder().id(taxEstimate.userId()).build())
            .referenceMonth(taxEstimate.referenceMonth().atDay(1))
            .regime(taxEstimate.regime())
            .grossRevenue(taxEstimate.grossRevenue())
            .appliedRate(taxEstimate.appliedRate())
            .estimatedValue(taxEstimate.estimatedValue())
            .build();
    }

    public TaxEstimate toDomain(TaxEstimateJpaEntity entity) {
        return new TaxEstimate(
            entity.getId(),
            entity.getUser().getId(),
            YearMonth.from(entity.getReferenceMonth()),
            entity.getRegime(),
            entity.getGrossRevenue(),
            entity.getAppliedRate(),
            entity.getEstimatedValue()
        );
    }
}
