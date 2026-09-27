package com.lmf.finpro.integration.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.ClientWorkType;
import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.infrastructure.web.dto.account.AccountRequest;
import com.lmf.finpro.infrastructure.web.dto.account.AccountResponse;
import com.lmf.finpro.infrastructure.web.dto.client.ClientAnalyticsResponse;
import com.lmf.finpro.infrastructure.web.dto.client.ClientRequest;
import com.lmf.finpro.infrastructure.web.dto.client.ClientResponse;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionRequest;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionResponse;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.CnpjTestFactory;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

class ClientAnalyticsIntegrationTest extends AbstractIntegrationTest {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

    @Test
    void ranksClientsAndFlagsHighConcentration() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);
        Long bigClient = createClient(user, "Cliente grande");
        Long smallClient = createClient(user, "Cliente pequeno");
        createTransaction(user, accountId, bigClient, "7000", TODAY, CategoryType.INCOME);
        createTransaction(
                user, accountId, bigClient, "7000", TODAY.minusMonths(1), CategoryType.INCOME);
        createTransaction(user, accountId, smallClient, "2000", TODAY, CategoryType.INCOME);
        createTransaction(user, accountId, smallClient, "300", TODAY, CategoryType.EXPENSE);

        ClientAnalyticsResponse analytics = get(user, "/api/clients/analytics?months=3").getBody();

        assertThat(analytics.months()).hasSize(3);
        assertThat(analytics.totalIncome()).isEqualByComparingTo("16000");
        assertThat(analytics.activeClients()).isEqualTo(2);
        assertThat(analytics.risk()).isEqualTo("HIGH");
        assertThat(analytics.topClientShare()).isEqualByComparingTo("0.875");
        assertThat(analytics.ranking()).hasSize(2);
        ClientAnalyticsResponse.ClientRow first = analytics.ranking().get(0);
        assertThat(first.name()).isEqualTo("Cliente grande");
        assertThat(first.incomeCount()).isEqualTo(2);
        assertThat(first.averageTicket()).isEqualByComparingTo("7000");
        assertThat(first.monthly()).hasSize(3);
        assertThat(analytics.ranking().get(1).net()).isEqualByComparingTo("1700");
    }

    @Test
    void anotherUserSeesNothingAndInvalidPeriodIsRejected() {
        TestUser owner = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(owner);
        Long client = createClient(owner, "Cliente");
        createTransaction(owner, accountId, client, "1000", TODAY, CategoryType.INCOME);

        TestUser other = TestDataFactory.registerRandomUser(restTemplate);
        ClientAnalyticsResponse analytics = get(other, "/api/clients/analytics").getBody();
        assertThat(analytics.months()).hasSize(12);
        assertThat(analytics.ranking()).isEmpty();
        assertThat(analytics.risk()).isEqualTo("NONE");

        assertThat(get(owner, "/api/clients/analytics?months=0").getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<ClientAnalyticsResponse> get(TestUser user, String url) {
        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(user.authHeaders()),
                ClientAnalyticsResponse.class);
    }

    private Long createAccount(TestUser user) {
        return restTemplate
                .exchange(
                        "/api/accounts",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new AccountRequest("Conta", AccountType.CHECKING, BigDecimal.ZERO),
                                user.authHeaders()),
                        AccountResponse.class)
                .getBody()
                .id();
    }

    private Long createClient(TestUser user, String name) {
        ResponseEntity<ClientResponse> response =
                restTemplate.exchange(
                        "/api/clients",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new ClientRequest(
                                        name,
                                        "contato@cliente.com",
                                        "11999998888",
                                        DocumentType.CNPJ,
                                        CnpjTestFactory.randomValidCnpj(),
                                        ClientWorkType.PJ,
                                        null,
                                        null,
                                        true),
                                user.authHeaders()),
                        ClientResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().id();
    }

    private void createTransaction(
            TestUser user,
            Long accountId,
            Long clientId,
            String amount,
            LocalDate date,
            CategoryType type) {
        ResponseEntity<TransactionResponse> response =
                restTemplate.exchange(
                        "/api/transactions",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new TransactionRequest(
                                        accountId,
                                        null,
                                        clientId,
                                        "Movimento",
                                        new BigDecimal(amount),
                                        date,
                                        type),
                                user.authHeaders()),
                        TransactionResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
