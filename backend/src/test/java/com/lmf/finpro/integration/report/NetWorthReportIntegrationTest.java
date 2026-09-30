package com.lmf.finpro.integration.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.DebtType;
import com.lmf.finpro.infrastructure.web.dto.account.AccountRequest;
import com.lmf.finpro.infrastructure.web.dto.account.AccountResponse;
import com.lmf.finpro.infrastructure.web.dto.debt.DebtCreateRequest;
import com.lmf.finpro.infrastructure.web.dto.debt.DebtResponse;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class NetWorthReportIntegrationTest extends AbstractIntegrationTest {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

    @Test
    void generatesTheNetWorthReportAsPdfByDefault() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        ResponseEntity<byte[]> response = download(user, "/api/reports/net-worth");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("attachment")
                .contains("evolucao-patrimonial-" + TODAY + ".pdf");
        assertThat(new String(response.getBody(), 0, 4, StandardCharsets.ISO_8859_1))
                .isEqualTo("%PDF");
    }

    @Test
    void csvMatchesTheNetWorthShownOnScreen() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        post(
                user,
                "/api/accounts",
                new AccountRequest("Corrente", AccountType.CHECKING, new BigDecimal("2000")),
                AccountResponse.class);
        post(
                user,
                "/api/debts",
                new DebtCreateRequest(
                        "Carro", DebtType.FINANCING, "Banco", new BigDecimal("300"), TODAY),
                DebtResponse.class);

        ResponseEntity<byte[]> response =
                download(user, "/api/reports/net-worth?months=3&format=CSV");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString()).startsWith("text/csv");
        String csv = new String(response.getBody(), StandardCharsets.UTF_8);
        YearMonth current = YearMonth.from(TODAY);
        assertThat(csv)
                .contains("Mês;Contas;Investimentos;Dívidas;Patrimônio líquido;Variação")
                .contains(current.minusMonths(2) + ";")
                .contains(current + ";2000.00;0;300.00;1700.00;")
                .contains("Corrente;BRL;2000.00;2000.00")
                .contains("Carro;Financiamento;Banco;300.00;" + TODAY);
    }

    @Test
    void rejectsAnOutOfRangePeriod() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        assertThat(download(user, "/api/reports/net-worth?months=0").getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(download(user, "/api/reports/net-worth?months=61").getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void requiresAuthentication() {
        ResponseEntity<byte[]> response =
                restTemplate.getForEntity("/api/reports/net-worth", byte[].class);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }

    private ResponseEntity<byte[]> download(TestUser user, String url) {
        return restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(user.authHeaders()), byte[].class);
    }

    private <T> void post(TestUser user, String url, Object body, Class<T> type) {
        assertThat(
                        restTemplate
                                .exchange(
                                        url,
                                        HttpMethod.POST,
                                        new HttpEntity<>(body, user.authHeaders()),
                                        type)
                                .getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
    }
}
