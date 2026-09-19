package com.lmf.finpro.integration.client;

import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.ClientWorkType;
import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.infrastructure.web.dto.account.AccountRequest;
import com.lmf.finpro.infrastructure.web.dto.account.AccountResponse;
import com.lmf.finpro.infrastructure.web.dto.client.ClientRequest;
import com.lmf.finpro.infrastructure.web.dto.client.ClientResponse;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionRequest;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionResponse;
import com.lmf.finpro.infrastructure.web.exception.ApiError;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.CnpjTestFactory;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createsListsUpdatesAndDeletesOwnClient() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        String cnpj = CnpjTestFactory.randomValidCnpj();

        ClientRequest createRequest = new ClientRequest(
            "Acme Ltda", "contato@acme.com", "11999998888",
            DocumentType.CNPJ, cnpj, ClientWorkType.PJ, "Cliente recorrente", "#3366FF", true
        );
        ResponseEntity<ClientResponse> createResponse = restTemplate.exchange(
            "/api/clients", HttpMethod.POST, new HttpEntity<>(createRequest, user.authHeaders()), ClientResponse.class
        );
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long clientId = createResponse.getBody().id();

        ResponseEntity<ClientResponse[]> listResponse = restTemplate.exchange(
            "/api/clients", HttpMethod.GET, new HttpEntity<>(user.authHeaders()), ClientResponse[].class
        );
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(List.of(listResponse.getBody())).extracting(ClientResponse::id).contains(clientId);

        ClientRequest updateRequest = new ClientRequest(
            "Acme S.A.", "financeiro@acme.com", "11999998888",
            DocumentType.CNPJ, cnpj, ClientWorkType.PJ, "Renomeado", "#FF0000", true
        );
        ResponseEntity<ClientResponse> updateResponse = restTemplate.exchange(
            "/api/clients/" + clientId, HttpMethod.PUT, new HttpEntity<>(updateRequest, user.authHeaders()), ClientResponse.class
        );
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody().name()).isEqualTo("Acme S.A.");

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
            "/api/clients/" + clientId, HttpMethod.DELETE, new HttpEntity<>(user.authHeaders()), Void.class
        );
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void createsClientWithoutOptionalColorAndNotes() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        ClientRequest createRequest = new ClientRequest(
            "Cliente sem cor nem observações", "cliente@example.com", "11988887777",
            DocumentType.CPF, "11144477735", ClientWorkType.AUTONOMO, null, null, true
        );
        ResponseEntity<ClientResponse> createResponse = restTemplate.exchange(
            "/api/clients", HttpMethod.POST, new HttpEntity<>(createRequest, user.authHeaders()), ClientResponse.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody().name()).isEqualTo("Cliente sem cor nem observações");
    }

    @Test
    void rejectsClientMissingRequiredFields() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        ClientRequest createRequest = new ClientRequest("Cliente incompleto", null, null, null, null, null, null, null, true);
        ResponseEntity<ApiError> createResponse = restTemplate.exchange(
            "/api/clients", HttpMethod.POST, new HttpEntity<>(createRequest, user.authHeaders()), ApiError.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void userCannotAccessAnotherUsersClient() {
        TestUser owner = TestDataFactory.registerRandomUser(restTemplate);
        TestUser intruder = TestDataFactory.registerRandomUser(restTemplate);

        ClientRequest createRequest = new ClientRequest(
            "Cliente da Ana", "ana.cliente@example.com", "11988887777",
            DocumentType.CPF, "11144477735", ClientWorkType.AUTONOMO, null, null, true
        );
        ResponseEntity<ClientResponse> createResponse = restTemplate.exchange(
            "/api/clients", HttpMethod.POST, new HttpEntity<>(createRequest, owner.authHeaders()), ClientResponse.class
        );
        Long clientId = createResponse.getBody().id();

        ResponseEntity<ApiError> getResponse = restTemplate.exchange(
            "/api/clients/" + clientId, HttpMethod.GET, new HttpEntity<>(intruder.authHeaders()), ApiError.class
        );

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void cannotDeleteClientWithLinkedTransactions() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        AccountRequest accountRequest = new AccountRequest("Conta corrente", AccountType.CHECKING, BigDecimal.valueOf(1000));
        ResponseEntity<AccountResponse> accountResponse = restTemplate.exchange(
            "/api/accounts", HttpMethod.POST, new HttpEntity<>(accountRequest, user.authHeaders()), AccountResponse.class
        );
        Long accountId = accountResponse.getBody().id();

        ClientRequest clientRequest = new ClientRequest(
            "Cliente vinculado", "vinculado@example.com", "11988887777",
            DocumentType.CPF, "11144477735", ClientWorkType.AUTONOMO, null, null, true
        );
        ResponseEntity<ClientResponse> clientResponse = restTemplate.exchange(
            "/api/clients", HttpMethod.POST, new HttpEntity<>(clientRequest, user.authHeaders()), ClientResponse.class
        );
        Long clientId = clientResponse.getBody().id();

        TransactionRequest transactionRequest = new TransactionRequest(
            accountId, null, clientId, "Serviço prestado", BigDecimal.valueOf(300), LocalDate.now(), CategoryType.INCOME
        );
        ResponseEntity<TransactionResponse> transactionResponse = restTemplate.exchange(
            "/api/transactions", HttpMethod.POST, new HttpEntity<>(transactionRequest, user.authHeaders()), TransactionResponse.class
        );
        assertThat(transactionResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<ApiError> deleteResponse = restTemplate.exchange(
            "/api/clients/" + clientId, HttpMethod.DELETE, new HttpEntity<>(user.authHeaders()), ApiError.class
        );
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
