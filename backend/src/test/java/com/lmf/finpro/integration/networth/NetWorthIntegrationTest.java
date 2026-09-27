package com.lmf.finpro.integration.networth;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.DebtType;
import com.lmf.finpro.infrastructure.web.dto.account.AccountRequest;
import com.lmf.finpro.infrastructure.web.dto.account.AccountResponse;
import com.lmf.finpro.infrastructure.web.dto.account.AccountValuationRequest;
import com.lmf.finpro.infrastructure.web.dto.account.AccountValuationResponse;
import com.lmf.finpro.infrastructure.web.dto.dashboard.DashboardOverviewResponse;
import com.lmf.finpro.infrastructure.web.dto.debt.DebtBalanceRequest;
import com.lmf.finpro.infrastructure.web.dto.debt.DebtBalanceResponse;
import com.lmf.finpro.infrastructure.web.dto.debt.DebtCreateRequest;
import com.lmf.finpro.infrastructure.web.dto.debt.DebtResponse;
import com.lmf.finpro.infrastructure.web.dto.networth.NetWorthResponse;
import com.lmf.finpro.infrastructure.web.dto.transfer.TransferRequest;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class NetWorthIntegrationTest extends AbstractIntegrationTest {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

    @Test
    void netWorthCombinesCashMarketValueOfInvestmentsAndDebts() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long checking = createAccount(user, "Corrente", AccountType.CHECKING, "2000");
        Long broker = createAccount(user, "Corretora", AccountType.INVESTMENT, "0");
        assertThat(transfer(user, checking, broker, "1000").getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        ResponseEntity<AccountValuationResponse> valuation =
                post(
                        user,
                        "/api/accounts/" + broker + "/valuations",
                        new AccountValuationRequest(TODAY, new BigDecimal("1100")),
                        AccountValuationResponse.class);
        assertThat(valuation.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        // O saldo da conta de investimento passa a ser o valor de mercado.
        assertThat(accountBalance(user, broker)).isEqualByComparingTo("1100");

        ResponseEntity<DebtResponse> debt =
                post(
                        user,
                        "/api/debts",
                        new DebtCreateRequest(
                                "Carro", DebtType.FINANCING, "Banco", new BigDecimal("300"), TODAY),
                        DebtResponse.class);
        assertThat(debt.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(debt.getBody().currentBalance()).isEqualByComparingTo("300");

        NetWorthResponse netWorth = get(user, "/api/net-worth?months=3", NetWorthResponse.class);
        assertThat(netWorth.history()).hasSize(3);
        assertThat(netWorth.current().cash()).isEqualByComparingTo("1000");
        assertThat(netWorth.current().investments()).isEqualByComparingTo("1100");
        assertThat(netWorth.current().debts()).isEqualByComparingTo("300");
        assertThat(netWorth.current().netWorth()).isEqualByComparingTo("1800");
        NetWorthResponse.InvestmentRow investment = netWorth.investments().get(0);
        assertThat(investment.invested()).isEqualByComparingTo("1000");
        assertThat(investment.gain()).isEqualByComparingTo("100");
        assertThat(investment.gainRate()).isEqualByComparingTo("0.1");
        assertThat(investment.lastValuationDate()).isEqualTo(TODAY);
        assertThat(netWorth.debts().get(0).name()).isEqualTo("Carro");

        // O Dashboard soma o rendimento no saldo consolidado, como a tela de Contas.
        assertThat(
                        get(user, "/api/dashboard/overview", DashboardOverviewResponse.class)
                                .currentBalance())
                .isEqualByComparingTo("2100");
        // O resgate de tudo, rendimento incluído, não é barrado por saldo insuficiente.
        assertThat(transfer(user, broker, checking, "1100").getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(accountBalance(user, broker)).isEqualByComparingTo("0");
    }

    @Test
    void valuationRulesAndDebtBalanceHistory() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long checking = createAccount(user, "Corrente", AccountType.CHECKING, "0");
        Long broker = createAccount(user, "Corretora", AccountType.INVESTMENT, "0");

        assertThat(
                        post(
                                        user,
                                        "/api/accounts/" + checking + "/valuations",
                                        new AccountValuationRequest(TODAY, BigDecimal.TEN),
                                        String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(
                        post(
                                        user,
                                        "/api/accounts/" + broker + "/valuations",
                                        new AccountValuationRequest(
                                                TODAY.plusDays(1), BigDecimal.TEN),
                                        String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        post(
                user,
                "/api/accounts/" + broker + "/valuations",
                new AccountValuationRequest(TODAY, BigDecimal.TEN),
                AccountValuationResponse.class);
        // Com valor de mercado informado, a conta não pode deixar de ser de investimento.
        assertThat(
                        restTemplate
                                .exchange(
                                        "/api/accounts/" + broker,
                                        HttpMethod.PUT,
                                        new HttpEntity<>(
                                                new AccountRequest(
                                                        "Corretora",
                                                        AccountType.CHECKING,
                                                        BigDecimal.ZERO),
                                                user.authHeaders()),
                                        String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        Long debtId =
                post(
                                user,
                                "/api/debts",
                                new DebtCreateRequest(
                                        "Cartão",
                                        DebtType.CREDIT_CARD,
                                        null,
                                        new BigDecimal("500"),
                                        TODAY.minusDays(10)),
                                DebtResponse.class)
                        .getBody()
                        .id();
        post(
                user,
                "/api/debts/" + debtId + "/balances",
                new DebtBalanceRequest(TODAY, BigDecimal.ZERO),
                DebtBalanceResponse.class);
        List<DebtBalanceResponse> balances =
                restTemplate
                        .exchange(
                                "/api/debts/" + debtId + "/balances",
                                HttpMethod.GET,
                                new HttpEntity<>(user.authHeaders()),
                                new ParameterizedTypeReference<List<DebtBalanceResponse>>() {})
                        .getBody();
        assertThat(balances)
                .extracting(DebtBalanceResponse::balanceDate)
                .containsExactly(TODAY, TODAY.minusDays(10));
        assertThat(get(user, "/api/net-worth", NetWorthResponse.class).current().debts())
                .isEqualByComparingTo("0");

        TestUser other = TestDataFactory.registerRandomUser(restTemplate);
        assertThat(
                        restTemplate
                                .exchange(
                                        "/api/debts/" + debtId,
                                        HttpMethod.DELETE,
                                        new HttpEntity<>(other.authHeaders()),
                                        String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(
                        restTemplate
                                .exchange(
                                        "/api/net-worth?months=0",
                                        HttpMethod.GET,
                                        new HttpEntity<>(user.authHeaders()),
                                        String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
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

    private Long createAccount(TestUser user, String name, AccountType type, String initial) {
        return post(
                        user,
                        "/api/accounts",
                        new AccountRequest(name, type, new BigDecimal(initial)),
                        AccountResponse.class)
                .getBody()
                .id();
    }

    private BigDecimal accountBalance(TestUser user, Long accountId) {
        return get(user, "/api/accounts/" + accountId, AccountResponse.class).currentBalance();
    }

    private ResponseEntity<String> transfer(TestUser user, Long from, Long to, String amount) {
        return post(
                user,
                "/api/transfers",
                new TransferRequest(from, to, new BigDecimal(amount), TODAY, "Movimentação"),
                String.class);
    }
}
