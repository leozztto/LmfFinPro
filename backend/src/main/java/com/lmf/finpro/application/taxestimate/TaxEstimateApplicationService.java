package com.lmf.finpro.application.taxestimate;

import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.TaxEstimate;
import com.lmf.finpro.domain.model.TaxRateEstimator;
import com.lmf.finpro.domain.model.TaxRegime;
import com.lmf.finpro.domain.port.out.TaxEstimateRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaxEstimateApplicationService {

    private final TaxEstimateRepositoryPort taxEstimateRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;

    public TaxEstimate create(
            Long currentUserId,
            YearMonth referenceMonth,
            TaxRegime regime,
            BigDecimal grossRevenue,
            BigDecimal appliedRate) {
        log.debug(
                "Criando estimativa de imposto do mês={} para o usuário={}",
                referenceMonth,
                currentUserId);
        return taxEstimateRepositoryPort.save(
                TaxEstimate.create(
                        currentUserId, referenceMonth, regime, grossRevenue, appliedRate));
    }

    public List<TaxEstimate> list(Long currentUserId) {
        log.debug("Listando estimativas de imposto do usuário={}", currentUserId);
        return taxEstimateRepositoryPort.findAllByUserId(currentUserId);
    }

    public BigDecimal suggestRate(TaxRegime regime, BigDecimal grossRevenue) {
        log.debug("Sugerindo alíquota para o regime={}", regime);
        return TaxRateEstimator.suggestRate(regime, grossRevenue);
    }

    /**
     * Receita do mês (em reais, sem transferências entre contas próprias): o ponto de partida da
     * receita bruta, que o usuário ainda pode ajustar.
     */
    public BigDecimal suggestGrossRevenue(Long currentUserId, YearMonth referenceMonth) {
        log.debug(
                "Sugerindo receita bruta do mês={} para o usuário={}",
                referenceMonth,
                currentUserId);
        return transactionRepositoryPort.sumBaseAmountByUserIdAndTypeBetween(
                currentUserId,
                CategoryType.INCOME,
                referenceMonth.atDay(1),
                referenceMonth.plusMonths(1).atDay(1));
    }

    public void delete(Long currentUserId, Long taxEstimateId) {
        log.debug("Removendo estimativa de imposto={} do usuário={}", taxEstimateId, currentUserId);
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
