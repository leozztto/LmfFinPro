package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.domain.model.Currency;
import com.lmf.finpro.infrastructure.persistence.entity.ExchangeRateJpaEntity;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRateJpaRepository extends JpaRepository<ExchangeRateJpaEntity, Long> {
    Optional<ExchangeRateJpaEntity> findFirstByCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(
            Currency currency, LocalDate date);

    List<ExchangeRateJpaEntity> findByCurrencyAndRateDateBetweenOrderByRateDate(
            Currency currency, LocalDate from, LocalDate to);

    List<ExchangeRateJpaEntity> findByCurrencyAndRateDateIn(
            Currency currency, Collection<LocalDate> dates);
}
