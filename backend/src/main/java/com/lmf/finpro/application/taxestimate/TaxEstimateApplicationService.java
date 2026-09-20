package com.lmf.finpro.application.taxestimate;

import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.TaxEstimate;
import com.lmf.finpro.domain.model.TaxRateEstimator;
import com.lmf.finpro.domain.model.TaxRegime;
import com.lmf.finpro.domain.port.out.TaxEstimateRepositoryPort;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaxEstimateApplicationService {

    private final TaxEstimateRepositoryPort taxEstimateRepositoryPort;

    public TaxEstimate create(
            Long currentUserId,
            YearMonth referenceMonth,
            TaxRegime regime,
            BigDecimal grossRevenue,
            BigDecimal appliedRate) {
        return taxEstimateRepositoryPort.save(
                TaxEstimate.create(
                        currentUserId, referenceMonth, regime, grossRevenue, appliedRate));
    }

    public List<TaxEstimate> list(Long currentUserId) {
        return taxEstimateRepositoryPort.findAllByUserId(currentUserId);
    }

    public BigDecimal suggestRate(TaxRegime regime, BigDecimal grossRevenue) {
        return TaxRateEstimator.suggestRate(regime, grossRevenue);
    }

    public void delete(Long currentUserId, Long taxEstimateId) {
        findOwnedOrThrow(currentUserId, taxEstimateId);
        taxEstimateRepositoryPort.deleteById(taxEstimateId);
    }

    /** Acesso a estimativa de outro usuário é tratado como inexistente (404), não como 403. */
    private TaxEstimate findOwnedOrThrow(Long currentUserId, Long taxEstimateId) {
        return taxEstimateRepositoryPort
                .findById(taxEstimateId)
                .filter(taxEstimate -> taxEstimate.belongsTo(currentUserId))
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Estimativa de imposto não encontrada: " + taxEstimateId));
    }
}
