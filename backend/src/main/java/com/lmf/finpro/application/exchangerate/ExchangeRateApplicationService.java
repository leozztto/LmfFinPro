package com.lmf.finpro.application.exchangerate;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.lmf.finpro.application.FlowLog;
import com.lmf.finpro.domain.exception.ExchangeRateUnavailableException;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.Currency;
import com.lmf.finpro.domain.model.ExchangeRate;
import com.lmf.finpro.domain.model.ExchangeRates;
import com.lmf.finpro.domain.model.RecurringTransaction;
import com.lmf.finpro.domain.port.out.ExchangeRateProviderPort;
import com.lmf.finpro.domain.port.out.ExchangeRateRepositoryPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Cotações em reais (PTAX do Banco Central), guardadas no banco. O que falta é buscado na hora na
 * fonte externa; se ela não responder, vale a última cotação guardada. Uma busca que falhou só é
 * repetida depois de {@link #RETRY_AFTER}, para as telas não ficarem esperando o Banco Central a
 * cada requisição.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateApplicationService {

    static final Duration RETRY_AFTER = Duration.ofHours(1);

    /** Janela buscada para trás: cobre fins de semana e feriados emendados. */
    private static final int LOOKBACK_DAYS = 10;

    private static final int RATE_SCALE = 6;

    private final ExchangeRateRepositoryPort exchangeRateRepositoryPort;
    private final ExchangeRateProviderPort exchangeRateProviderPort;
    private final Clock clock;

    /** Última busca por moeda e o período que ela cobriu. */
    private final Map<Currency, Attempt> lastAttempts = new ConcurrentHashMap<>();

    private record Attempt(Instant at, LocalDate from, LocalDate to) {}

    /**
     * @param rate quantas unidades da moeda de destino vale uma da de origem
     * @param amount valor convertido, arredondado em centavos
     */
    public record Conversion(BigDecimal rate, BigDecimal amount) {}

    /** Quantos reais vale uma unidade da moeda no dia (data futura usa a cotação de hoje). */
    public BigDecimal rateOn(Currency currency, LocalDate date) {
        if (currency.isBase()) {
            return BigDecimal.ONE;
        }
        LocalDate effectiveDate = date.isAfter(today()) ? today() : date;
        Optional<ExchangeRate> stored =
                exchangeRateRepositoryPort.findLatestOnOrBefore(currency, effectiveDate);
        if (stored.isPresent() && stored.get().rateDate().isEqual(effectiveDate)) {
            return stored.get().rate();
        }
        LocalDate lookbackStart = effectiveDate.minusDays(LOOKBACK_DAYS);
        LocalDate fetchFrom =
                stored.map(rate -> rate.rateDate().plusDays(1))
                        .filter(from -> from.isAfter(lookbackStart))
                        .orElse(lookbackStart);
        refresh(currency, fetchFrom, effectiveDate);
        return exchangeRateRepositoryPort
                .findLatestOnOrBefore(currency, effectiveDate)
                .map(ExchangeRate::rate)
                .orElseThrow(
                        () ->
                                new ExchangeRateUnavailableException(
                                        "Cotação do "
                                                + currency
                                                + " indisponível no momento. Tente de novo mais"
                                                + " tarde."));
    }

    /** Valor em reais na cotação do dia, arredondado em centavos. */
    public BigDecimal toBrl(Currency currency, BigDecimal amount, LocalDate date) {
        if (currency.isBase()) {
            return amount;
        }
        return amount.multiply(rateOn(currency, date)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Valor em reais pela última cotação, para exibição (ex.: o saldo atual de uma conta em dólar);
     * {@code null} se não houver cotação — a tela mostra só o valor na moeda original.
     */
    public BigDecimal toBrlTodayOrNull(Currency currency, BigDecimal amount) {
        if (currency.isBase()) {
            return amount;
        }
        try {
            return toBrl(currency, amount, today());
        } catch (ExchangeRateUnavailableException ex) {
            return null;
        }
    }

    /**
     * Recorrências com o valor em reais (as de conta em outra moeda, pela cotação de hoje), para
     * previsões — projeção de caixa e calendário. Não são salvas.
     */
    public List<RecurringTransaction> recurrencesInBrl(
            List<Account> accounts, List<RecurringTransaction> recurrences) {
        Map<Long, Currency> currencyByAccount =
                accounts.stream().collect(Collectors.toMap(Account::id, Account::currency));
        return recurrences.stream()
                .map(
                        recurrence -> {
                            Currency currency =
                                    currencyByAccount.getOrDefault(
                                            recurrence.accountId(), Currency.BRL);
                            return currency.isBase()
                                    ? recurrence
                                    : recurrence.withAmount(
                                            toBrl(currency, recurrence.amount(), today()));
                        })
                .toList();
    }

    /** Conversão entre duas moedas quaisquer, passando pelo real. */
    public Conversion convert(Currency from, Currency to, BigDecimal amount, LocalDate date) {
        BigDecimal rate =
                from == to
                        ? BigDecimal.ONE
                        : rateOn(from, date)
                                .divide(rateOn(to, date), RATE_SCALE, RoundingMode.HALF_UP);
        return new Conversion(rate, amount.multiply(rate).setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * Cotações das moedas entre {@code from} e {@code to} (e alguns dias antes, para os primeiros
     * dias sem cotação), para converter muitas datas de uma vez — ex.: o histórico do patrimônio.
     */
    public ExchangeRates table(Collection<Currency> currencies, LocalDate from, LocalDate to) {
        LocalDate until = to.isAfter(today()) ? today() : to;
        LocalDate windowStart = from.minusDays(LOOKBACK_DAYS);
        List<ExchangeRate> rates = new ArrayList<>();
        for (Currency currency : currencies.stream().distinct().toList()) {
            if (currency.isBase()) {
                continue;
            }
            // Garante as cotações recentes; se nem assim houver alguma, não há o que converter.
            rateOn(currency, until);
            List<ExchangeRate> stored =
                    exchangeRateRepositoryPort.findAllBetween(currency, windowStart, until);
            if (stored.isEmpty() || stored.get(0).rateDate().isAfter(from)) {
                LocalDate fetchUntil = stored.isEmpty() ? until : stored.get(0).rateDate();
                refresh(currency, windowStart, fetchUntil);
                stored = exchangeRateRepositoryPort.findAllBetween(currency, windowStart, until);
            }
            rates.addAll(stored);
            if (stored.isEmpty()) {
                exchangeRateRepositoryPort
                        .findLatestOnOrBefore(currency, until)
                        .ifPresent(rates::add);
            }
        }
        return new ExchangeRates(rates);
    }

    /** Busca as cotações recentes de todas as moedas estrangeiras (agendador diário). */
    public int refreshRecent() {
        int saved = 0;
        for (Currency currency : Currency.values()) {
            if (!currency.isBase()) {
                lastAttempts.remove(currency);
                saved += refresh(currency, today().minusDays(LOOKBACK_DAYS), today());
            }
        }
        FlowLog.detail("rates", saved);
        return saved;
    }

    /**
     * Busca na fonte externa, a menos que o mesmo período já tenha sido buscado há pouco (a cotação
     * do dia só sai à tarde: sem isso, toda tela da manhã consultaria o Banco Central).
     */
    private int refresh(Currency currency, LocalDate from, LocalDate to) {
        Attempt last = lastAttempts.get(currency);
        Instant now = clock.instant();
        if (last != null
                && !last.from().isAfter(from)
                && !last.to().isBefore(to)
                && Duration.between(last.at(), now).compareTo(RETRY_AFTER) < 0) {
            log.debug("Cotação do {} já buscada há pouco; consulta externa pulada", currency);
            return 0;
        }
        lastAttempts.put(currency, new Attempt(now, from, to));
        long start = System.nanoTime();
        try {
            List<ExchangeRate> fetched = exchangeRateProviderPort.fetch(currency, from, to);
            if (!fetched.isEmpty()) {
                exchangeRateRepositoryPort.saveAll(fetched);
            }
            log.info(
                    "Cotações buscadas na fonte externa {} {} {} {} {}",
                    kv("currency", currency),
                    kv("from", from),
                    kv("to", to),
                    kv("fetched", fetched.size()),
                    kv("durationMs", (System.nanoTime() - start) / 1_000_000));
            return fetched.size();
        } catch (ExchangeRateUnavailableException ex) {
            log.warn(
                    "Falha ao buscar a cotação do {} de {} a {}: {}",
                    currency,
                    from,
                    to,
                    ex.getMessage());
            return 0;
        }
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
