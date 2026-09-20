package com.lmf.finpro.infrastructure.web.mapper;

import com.lmf.finpro.domain.model.TaxEstimate;
import com.lmf.finpro.infrastructure.web.dto.taxestimate.TaxEstimateResponse;
import org.springframework.stereotype.Component;

@Component
public class TaxEstimateWebMapper {

    public TaxEstimateResponse toResponse(TaxEstimate taxEstimate) {
        return new TaxEstimateResponse(
                taxEstimate.id(),
                taxEstimate.referenceMonth(),
                taxEstimate.regime(),
                taxEstimate.grossRevenue(),
                taxEstimate.appliedRate(),
                taxEstimate.estimatedValue());
    }
}
