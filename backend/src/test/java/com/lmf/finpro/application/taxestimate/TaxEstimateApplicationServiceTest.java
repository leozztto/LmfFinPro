package com.lmf.finpro.application.taxestimate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.TaxEstimate;
import com.lmf.finpro.domain.model.TaxRegime;
import com.lmf.finpro.domain.port.out.TaxEstimateRepositoryPort;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaxEstimateApplicationServiceTest {

    @Mock private TaxEstimateRepositoryPort taxEstimateRepositoryPort;

    @InjectMocks private TaxEstimateApplicationService service;

    @Test
    void createComputesEstimatedValueAsRevenueTimesRate() {
        when(taxEstimateRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TaxEstimate created =
                service.create(
                        10L,
                        YearMonth.of(2026, 9),
                        TaxRegime.SIMPLES_NACIONAL,
                        BigDecimal.valueOf(5000),
                        new BigDecimal("0.06"));

        assertThat(created.estimatedValue()).isEqualByComparingTo("300.00");
    }

    @Test
    void listReturnsAllEstimatesForUser() {
        TaxEstimate estimate =
                TaxEstimate.create(
                        10L,
                        YearMonth.of(2026, 9),
                        TaxRegime.MEI,
                        BigDecimal.valueOf(2000),
                        new BigDecimal("0.06"));
        when(taxEstimateRepositoryPort.findAllByUserId(10L)).thenReturn(List.of(estimate));

        assertThat(service.list(10L)).containsExactly(estimate);
    }

    @Test
    void suggestRateDelegatesToTaxRateEstimator() {
        BigDecimal rate = service.suggestRate(TaxRegime.MEI, BigDecimal.valueOf(5000));

        assertThat(rate).isEqualByComparingTo("0.06");
    }

    @Test
    void deleteRemovesEstimateWhenOwned() {
        TaxEstimate estimate =
                new TaxEstimate(
                        1L,
                        10L,
                        YearMonth.of(2026, 9),
                        TaxRegime.MEI,
                        BigDecimal.valueOf(2000),
                        new BigDecimal("0.06"),
                        new BigDecimal("120.00"));
        when(taxEstimateRepositoryPort.findById(1L)).thenReturn(Optional.of(estimate));

        service.delete(10L, 1L);

        verify(taxEstimateRepositoryPort).deleteById(1L);
    }

    @Test
    void deleteThrowsWhenEstimateBelongsToAnotherUser() {
        TaxEstimate estimate =
                new TaxEstimate(
                        1L,
                        10L,
                        YearMonth.of(2026, 9),
                        TaxRegime.MEI,
                        BigDecimal.valueOf(2000),
                        new BigDecimal("0.06"),
                        new BigDecimal("120.00"));
        when(taxEstimateRepositoryPort.findById(1L)).thenReturn(Optional.of(estimate));

        assertThatThrownBy(() -> service.delete(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(taxEstimateRepositoryPort, never()).deleteById(any());
    }
}
