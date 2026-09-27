package com.lmf.finpro.integration.currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.model.AccountScope;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Currency;
import com.lmf.finpro.domain.model.ExchangeRate;
import com.lmf.finpro.domain.port.out.ExchangeRateProviderPort;
import com.lmf.finpro.infrastructure.web.dto.account.AccountRequest;
import com.lmf.finpro.infrastructure.web.dto.account.AccountResponse;
import com.lmf.finpro.infrastructure.web.dto.dashboard.DashboardOverviewResponse;
import com.lmf.finpro.infrastructure.web.dto.exchangerate.ConversionResponse;
import com.lmf.finpro.infrastructure.web.dto.networth.NetWorthResponse;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionRequest;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionResponse;
import com.lmf.finpro.infrastructure.web.dto.transfer.TransferRequest;
import com.lmf.finpro.infrastructure.web.dto.transfer.TransferResponse;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Conta em dólar, compra em dólar no cartão em reais e remessa de dólar para real. */
class MultiCurrencyIntegrationTest extends AbstractIntegrationTest {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

    /** A PTAX simulada: US$ 1 = R$ 5,00 todos os dias. */
    @MockBean private ExchangeRateProviderPort exchangeRateProviderPort;

    @BeforeEach
    void stubRates() {
        when(exchangeRateProviderPort.fetch(any(), any(), any()))
                .thenAnswer(
                        invocation -> {
                            Currency currency = invocation.getArgument(0);
                            LocalDate from = invocation.getArgument(1);
                            LocalDate to = invocation.getArgument(2);
                            return Stream.iterate(
                                            from,
                                            date -> !date.isAfter(to),
                                            date -> date.plusDays(1))
                                    .map(
                                            date ->
                                                    new ExchangeRate(
                                                            currency, date, new BigDecimal("5.00")))
                                    .toList();
                        });
    }

    @Test
    void foreignAccountsAndOperationsAreConsolidatedInReais() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        AccountResponse wise =
                post(
                                user,
                                "/api/accounts",
                                new AccountRequest(
                                        "Wise",
                                        AccountType.CHECKING,
                                        new BigDecimal("100"),
                                        AccountScope.BUSINESS,
                                        Currency.USD),
                                AccountResponse.class)
                        .getBody();
        assertThat(wise.currency()).isEqualTo(Currency.USD);
        assertThat(wise.currentBalanceInBrl()).isEqualByComparingTo("500.00");
        Long nubank =
                post(
                                user,
                                "/api/accounts",
                                new AccountRequest("Nubank", AccountType.CHECKING, BigDecimal.ZERO),
                                AccountResponse.class)
                        .getBody()
                        .id();

        // Receita de US$ 1.000 na conta em dólar: vale R$ 5.000 no consolidado.
        TransactionResponse invoice =
                post(
                                user,
                                "/api/transactions",
                                new TransactionRequest(
                                        wise.id(),
                                        null,
                                        null,
                                        "Fatura cliente exterior",
                                        new BigDecimal("1000.00"),
                                        TODAY,
                                        CategoryType.INCOME),
                                TransactionResponse.class)
                        .getBody();
        assertThat(invoice.baseAmount()).isEqualByComparingTo("5000.00");
        assertThat(invoice.originalCurrency()).isNull();

        // Compra de US$ 20 no cartão em reais: a fatura cobrou R$ 118,40 (spread e IOF).
        TransactionResponse purchase =
                post(
                                user,
                                "/api/transactions",
                                new TransactionRequest(
                                        nubank,
                                        null,
                                        null,
                                        "Hospedagem do site",
                                        new BigDecimal("118.40"),
                                        TODAY,
                                        CategoryType.EXPENSE,
                                        null,
                                        List.of(),
                                        Currency.USD,
                                        new BigDecimal("20.00")),
                                TransactionResponse.class)
                        .getBody();
        assertThat(purchase.baseAmount()).isEqualByComparingTo("118.40");
        assertThat(purchase.originalCurrency()).isEqualTo(Currency.USD);
        assertThat(purchase.originalAmount()).isEqualByComparingTo("20.00");

        // Remessa: saem US$ 200 e entram R$ 990 (câmbio efetivo, abaixo da PTAX).
        assertThat(
                        post(
                                        user,
                                        "/api/transfers",
                                        new TransferRequest(
                                                wise.id(),
                                                nubank,
                                                new BigDecimal("200"),
                                                TODAY,
                                                null),
                                        String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        TransferResponse remittance =
                post(
                                user,
                                "/api/transfers",
                                new TransferRequest(
                                        wise.id(),
                                        nubank,
                                        new BigDecimal("200"),
                                        TODAY,
                                        null,
                                        new BigDecimal("990.00")),
                                TransferResponse.class)
                        .getBody();
        assertThat(remittance.receivedAmount()).isEqualByComparingTo("990.00");

        assertThat(balance(user, wise.id())).isEqualByComparingTo("900");
        assertThat(balance(user, nubank)).isEqualByComparingTo("871.60");

        // Dashboard: fluxos pelo valor em reais de cada transação; transferências se anulam.
        DashboardOverviewResponse overview =
                get(user, "/api/dashboard/overview", DashboardOverviewResponse.class);
        assertThat(overview.currentMonthIncome()).isEqualByComparingTo("5000.00");
        assertThat(overview.currentMonthExpense()).isEqualByComparingTo("118.40");
        assertThat(overview.currentBalance()).isEqualByComparingTo("5381.60");

        // Patrimônio: saldo em dólar pela cotação de hoje — aparece a perda de R$ 10 no câmbio.
        NetWorthResponse netWorth = get(user, "/api/net-worth?months=2", NetWorthResponse.class);
        assertThat(netWorth.current().cash()).isEqualByComparingTo("5371.60");
        assertThat(netWorth.accounts())
                .anySatisfy(
                        row -> {
                            assertThat(row.currency()).isEqualTo(Currency.USD);
                            assertThat(row.balance()).isEqualByComparingTo("900");
                            assertThat(row.balanceInBrl()).isEqualByComparingTo("4500.00");
                        });

        // Com lançamentos, a moeda da conta não muda mais.
        assertThat(
                        restTemplate
                                .exchange(
                                        "/api/accounts/" + wise.id(),
                                        HttpMethod.PUT,
                                        new HttpEntity<>(
                                                new AccountRequest(
                                                        "Wise",
                                                        AccountType.CHECKING,
                                                        new BigDecimal("100"),
                                                        AccountScope.BUSINESS,
                                                        Currency.EUR),
                                                user.authHeaders()),
                                        String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void convertSuggestsTheValueInAnotherCurrency() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        ConversionResponse conversion =
                get(
                        user,
                        "/api/exchange-rates/convert?from=USD&to=BRL&date=" + TODAY + "&amount=100",
                        ConversionResponse.class);

        assertThat(conversion.rate()).isEqualByComparingTo("5");
        assertThat(conversion.amount()).isEqualByComparingTo("500.00");
    }

    private BigDecimal balance(TestUser user, Long accountId) {
        return get(user, "/api/accounts/" + accountId, AccountResponse.class).currentBalance();
    }

    private <T> ResponseEntity<T> post(TestUser user, String url, Object body, Class<T> type) {
        return restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, user.authHeaders()), type);
    }

    private <T> T get(TestUser user, String url, Class<T> type) {
        return restTemplate
                .exchange(url, HttpMethod.GET, new HttpEntity<>(user.authHeaders()), type)
                .getBody();
    }
}
