package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.model.Currency;
import com.lmf.finpro.domain.model.ExchangeRate;
import com.lmf.finpro.domain.port.out.ExchangeRateRepositoryPort;
import com.lmf.finpro.infrastructure.persistence.entity.ExchangeRateJpaEntity;
import com.lmf.finpro.infrastructure.persistence.repository.ExchangeRateJpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ExchangeRateRepositoryAdapter implements ExchangeRateRepositoryPort {

    private final ExchangeRateJpaRepository repository;

    @Override
    @Transactional
    public void saveAll(List<ExchangeRate> rates) {
        Map<Currency, List<ExchangeRate>> byCurrency =
                rates.stream().collect(Collectors.groupingBy(ExchangeRate::currency));
        byCurrency.forEach(
                (currency, currencyRates) -> {
                    Map<LocalDate, ExchangeRateJpaEntity> existing =
                            repository
                                    .findByCurrencyAndRateDateIn(
                                            currency,
                                            currencyRates.stream()
                                                    .map(ExchangeRate::rateDate)
                                                    .toList())
                                    .stream()
                                    .collect(
                                            Collectors.toMap(
                                                    ExchangeRateJpaEntity::getRateDate,
                                                    Function.identity()));
                    for (ExchangeRate rate : currencyRates) {
                        ExchangeRateJpaEntity entity =
                                existing.getOrDefault(
                                        rate.rateDate(),
                                        ExchangeRateJpaEntity.builder()
                                                .currency(currency)
                                                .rateDate(rate.rateDate())
                                                .build());
                        entity.setRate(rate.rate());
                        repository.save(entity);
                    }
                });
    }

    @Override
    public Optional<ExchangeRate> findLatestOnOrBefore(Currency currency, LocalDate date) {
        return repository
                .findFirstByCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(currency, date)
                .map(this::toDomain);
    }

    @Override
    public List<ExchangeRate> findAllBetween(Currency currency, LocalDate from, LocalDate to) {
        return repository
                .findByCurrencyAndRateDateBetweenOrderByRateDate(currency, from, to)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private ExchangeRate toDomain(ExchangeRateJpaEntity entity) {
        return new ExchangeRate(entity.getCurrency(), entity.getRateDate(), entity.getRate());
    }
}
