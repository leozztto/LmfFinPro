package com.lmf.finpro.integration.taxestimate;

import com.lmf.finpro.domain.model.TaxRegime;
import com.lmf.finpro.infrastructure.web.dto.taxestimate.SuggestedRateResponse;
import com.lmf.finpro.infrastructure.web.dto.taxestimate.TaxEstimateRequest;
import com.lmf.finpro.infrastructure.web.dto.taxestimate.TaxEstimateResponse;
import com.lmf.finpro.infrastructure.web.exception.ApiError;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TaxEstimateIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createsListsAndDeletesOwnTaxEstimate() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        TaxEstimateRequest createRequest = new TaxEstimateRequest(
            YearMonth.now(), TaxRegime.SIMPLES_NACIONAL, BigDecimal.valueOf(5000), new BigDecimal("0.06")
        );
        ResponseEntity<TaxEstimateResponse> createResponse = restTemplate.exchange(
            "/api/tax-estimates", HttpMethod.POST, new HttpEntity<>(createRequest, user.authHeaders()), TaxEstimateResponse.class
        );
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody().estimatedValue()).isEqualByComparingTo("300.00");
        Long taxEstimateId = createResponse.getBody().id();

        ResponseEntity<TaxEstimateResponse[]> listResponse = restTemplate.exchange(
            "/api/tax-estimates", HttpMethod.GET, new HttpEntity<>(user.authHeaders()), TaxEstimateResponse[].class
        );
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(List.of(listResponse.getBody())).extracting(TaxEstimateResponse::id).contains(taxEstimateId);

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
            "/api/tax-estimates/" + taxEstimateId, HttpMethod.DELETE, new HttpEntity<>(user.authHeaders()), Void.class
        );
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void rejectsTaxEstimateWithNegativeGrossRevenue() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        TaxEstimateRequest createRequest = new TaxEstimateRequest(
            YearMonth.now(), TaxRegime.MEI, BigDecimal.valueOf(-100), new BigDecimal("0.06")
        );
        ResponseEntity<ApiError> createResponse = restTemplate.exchange(
            "/api/tax-estimates", HttpMethod.POST, new HttpEntity<>(createRequest, user.authHeaders()), ApiError.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void userCannotAccessAnotherUsersTaxEstimate() {
        TestUser owner = TestDataFactory.registerRandomUser(restTemplate);
        TestUser intruder = TestDataFactory.registerRandomUser(restTemplate);

        TaxEstimateRequest createRequest = new TaxEstimateRequest(
            YearMonth.now(), TaxRegime.MEI, BigDecimal.valueOf(2000), new BigDecimal("0.06")
        );
        ResponseEntity<TaxEstimateResponse> createResponse = restTemplate.exchange(
            "/api/tax-estimates", HttpMethod.POST, new HttpEntity<>(createRequest, owner.authHeaders()), TaxEstimateResponse.class
        );
        Long taxEstimateId = createResponse.getBody().id();

        ResponseEntity<ApiError> deleteResponse = restTemplate.exchange(
            "/api/tax-estimates/" + taxEstimateId, HttpMethod.DELETE, new HttpEntity<>(intruder.authHeaders()), ApiError.class
        );

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void suggestsRateByRegime() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        ResponseEntity<SuggestedRateResponse> meiResponse = restTemplate.exchange(
            "/api/tax-estimates/suggested-rate?regime=MEI&grossRevenue=5000",
            HttpMethod.GET, new HttpEntity<>(user.authHeaders()), SuggestedRateResponse.class
        );
        assertThat(meiResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meiResponse.getBody().rate()).isEqualByComparingTo("0.06");

        ResponseEntity<SuggestedRateResponse> autonomoBaixaRendaResponse = restTemplate.exchange(
            "/api/tax-estimates/suggested-rate?regime=AUTONOMO&grossRevenue=2000",
            HttpMethod.GET, new HttpEntity<>(user.authHeaders()), SuggestedRateResponse.class
        );
        assertThat(autonomoBaixaRendaResponse.getBody().rate()).isEqualByComparingTo("0");

        ResponseEntity<SuggestedRateResponse> autonomoAltaRendaResponse = restTemplate.exchange(
            "/api/tax-estimates/suggested-rate?regime=AUTONOMO&grossRevenue=10000",
            HttpMethod.GET, new HttpEntity<>(user.authHeaders()), SuggestedRateResponse.class
        );
        assertThat(autonomoAltaRendaResponse.getBody().rate()).isEqualByComparingTo("0.275");
    }
}
