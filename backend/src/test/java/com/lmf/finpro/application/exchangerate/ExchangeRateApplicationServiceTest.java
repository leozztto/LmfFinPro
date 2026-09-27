package com.lmf.finpro.application.exchangerate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.exception.ExchangeRateUnavailableException;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountScope;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Currency;
import com.lmf.finpro.domain.model.ExchangeRate;
import com.lmf.finpro.domain.model.ExchangeRates;
import com.lmf.finpro.domain.model.RecurrenceFrequency;
import com.lmf.finpro.domain.model.RecurringTransaction;
import com.lmf.finpro.domain.port.out.ExchangeRateProviderPort;
import com.lmf.finpro.domain.port.out.ExchangeRateRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExchangeRateApplicationServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    /** Segunda-feira, 21/09/2026, às 10h — antes de a PTAX do dia sair. */
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 21);

    private final InMemoryRates repository = new InMemoryRates();
    private final ExchangeRateProviderPort provider = mock(ExchangeRateProviderPort.class);
    private final MutableClock clock =
            new MutableClock(TODAY.atTime(10, 0).atZone(ZONE).toInstant());
    private ExchangeRateApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ExchangeRateApplicationService(repository, provider, clock);
    }

    private static ExchangeRate usd(String date, String rate) {
        return new ExchangeRate(Currency.USD, LocalDate.parse(date), new BigDecimal(rate));
    }

    @Test
    void realIsAlwaysOneAndNeverQueriesTheSource() {
        assertThat(service.rateOn(Currency.BRL, TODAY)).isEqualByComparingTo("1");
        assertThat(service.toBrl(Currency.BRL, new BigDecimal("10.00"), TODAY))
                .isEqualByComparingTo("10.00");
        verifyNoInteractions(provider);
    }

    @Test
    void storedRateOfTheDayIsUsedWithoutQueryingTheSource() {
        repository.saveAll(List.of(usd("2026-09-18", "5.1575")));

        assertThat(service.toBrl(Currency.USD, new BigDecimal("100"), LocalDate.of(2026, 9, 18)))
                .isEqualByComparingTo("515.75");
        verifyNoInteractions(provider);
    }

    @Test
    void missingRateIsFetchedAndSaved() {
        when(provider.fetch(eq(Currency.USD), any(), eq(LocalDate.of(2026, 9, 18))))
                .thenReturn(List.of(usd("2026-09-17", "5.10"), usd("2026-09-18", "5.20")));

        assertThat(service.rateOn(Currency.USD, LocalDate.of(2026, 9, 18)))
                .isEqualByComparingTo("5.20");
        assertThat(repository.rates).hasSize(2);
    }

    @Test
    void dayWithoutRateUsesThePreviousBusinessDay() {
        repository.saveAll(List.of(usd("2026-09-18", "5.20")));
        when(provider.fetch(any(), any(), any())).thenReturn(List.of());

        // Sábado: não há PTAX, vale a de sexta.
        assertThat(service.rateOn(Currency.USD, LocalDate.of(2026, 9, 19)))
                .isEqualByComparingTo("5.20");
    }

    @Test
    void futureDateUsesTheLatestRate() {
        repository.saveAll(List.of(usd("2026-09-18", "5.20")));
        when(provider.fetch(any(), any(), any())).thenReturn(List.of());

        assertThat(service.rateOn(Currency.USD, LocalDate.of(2026, 12, 25)))
                .isEqualByComparingTo("5.20");
        verify(provider).fetch(Currency.USD, LocalDate.of(2026, 9, 19), TODAY);
    }

    @Test
    void sourceDownFallsBackToTheLastStoredRate() {
        repository.saveAll(List.of(usd("2026-09-18", "5.20")));
        when(provider.fetch(any(), any(), any()))
                .thenThrow(new ExchangeRateUnavailableException("fora do ar"));

        assertThat(service.rateOn(Currency.USD, TODAY)).isEqualByComparingTo("5.20");
    }

    @Test
    void withoutAnyRateFails() {
        when(provider.fetch(any(), any(), any()))
                .thenThrow(new ExchangeRateUnavailableException("fora do ar"));

        assertThatThrownBy(() -> service.rateOn(Currency.EUR, TODAY))
                .isInstanceOf(ExchangeRateUnavailableException.class)
                .hasMessageContaining("EUR");
    }

    @Test
    void sameMissingPeriodIsNotQueriedAgainUntilAnHourPasses() {
        repository.saveAll(List.of(usd("2026-09-18", "5.20")));
        when(provider.fetch(any(), any(), any())).thenReturn(List.of());

        service.rateOn(Currency.USD, TODAY);
        service.rateOn(Currency.USD, TODAY);
        verify(provider, times(1)).fetch(any(), any(), any());

        clock.advance(ExchangeRateApplicationService.RETRY_AFTER.plusMinutes(1));
        service.rateOn(Currency.USD, TODAY);
        verify(provider, times(2)).fetch(any(), any(), any());
    }

    @Test
    void convertsBetweenTwoForeignCurrenciesThroughTheReal() {
        repository.saveAll(
                List.of(
                        usd("2026-09-18", "5.00"),
                        new ExchangeRate(
                                Currency.EUR, LocalDate.of(2026, 9, 18), new BigDecimal("6.00"))));

        ExchangeRateApplicationService.Conversion conversion =
                service.convert(
                        Currency.EUR,
                        Currency.USD,
                        new BigDecimal("100"),
                        LocalDate.of(2026, 9, 18));

        assertThat(conversion.rate()).isEqualByComparingTo("1.2");
        assertThat(conversion.amount()).isEqualByComparingTo("120.00");
    }

    @Test
    void tableFetchesTheOlderHistoryOnceAndConvertsEachDate() {
        repository.saveAll(List.of(usd("2026-09-18", "5.20")));
        when(provider.fetch(eq(Currency.USD), any(), eq(LocalDate.of(2026, 9, 18))))
                .thenReturn(List.of(usd("2026-06-30", "4.90"), usd("2026-09-18", "5.20")));

        ExchangeRates table =
                service.table(List.of(Currency.USD, Currency.BRL), LocalDate.of(2026, 7, 1), TODAY);

        assertThat(table.toBrl(Currency.USD, new BigDecimal("100"), LocalDate.of(2026, 7, 31)))
                .isEqualByComparingTo("490.00");
        assertThat(table.toBrl(Currency.USD, new BigDecimal("100"), TODAY))
                .isEqualByComparingTo("520.00");
    }

    @Test
    void recurrencesOfForeignAccountsAreConvertedAtTodaysRate() {
        repository.saveAll(List.of(usd("2026-09-21", "5.00")));
        Account usdAccount =
                new Account(
                        1L,
                        10L,
                        "Wise",
                        AccountType.CHECKING,
                        BigDecimal.ZERO,
                        null,
                        AccountScope.BUSINESS,
                        Currency.USD);
        RecurringTransaction recurrence =
                RecurringTransaction.create(
                        10L,
                        1L,
                        null,
                        null,
                        "Assinatura",
                        new BigDecimal("20.00"),
                        CategoryType.EXPENSE,
                        RecurrenceFrequency.MONTHLY,
                        TODAY,
                        null);

        List<RecurringTransaction> converted =
                service.recurrencesInBrl(List.of(usdAccount), List.of(recurrence));

        assertThat(converted.get(0).amount()).isEqualByComparingTo("100.00");
        verify(provider, never()).fetch(any(), any(), any());
    }

    /** Repositório em memória, com a mesma semântica do adapter JPA. */
    private static final class InMemoryRates implements ExchangeRateRepositoryPort {
        private final List<ExchangeRate> rates = new ArrayList<>();

        @Override
        public void saveAll(List<ExchangeRate> newRates) {
            for (ExchangeRate rate : newRates) {
                rates.removeIf(
                        existing ->
                                existing.currency() == rate.currency()
                                        && existing.rateDate().equals(rate.rateDate()));
                rates.add(rate);
            }
        }

        @Override
        public Optional<ExchangeRate> findLatestOnOrBefore(Currency currency, LocalDate date) {
            return rates.stream()
                    .filter(rate -> rate.currency() == currency && !rate.rateDate().isAfter(date))
                    .max(Comparator.comparing(ExchangeRate::rateDate));
        }

        @Override
        public List<ExchangeRate> findAllBetween(Currency currency, LocalDate from, LocalDate to) {
            return rates.stream()
                    .filter(rate -> rate.currency() == currency)
                    .filter(rate -> !rate.rateDate().isBefore(from) && !rate.rateDate().isAfter(to))
                    .sorted(Comparator.comparing(ExchangeRate::rateDate))
                    .toList();
        }
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZONE;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
