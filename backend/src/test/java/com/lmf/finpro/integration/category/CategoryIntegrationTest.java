package com.lmf.finpro.integration.category;

import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.infrastructure.web.dto.category.CategoryRequest;
import com.lmf.finpro.infrastructure.web.dto.category.CategoryResponse;
import com.lmf.finpro.infrastructure.web.exception.ApiError;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createsListsAndUpdatesOwnCategory() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        CategoryRequest createRequest = new CategoryRequest("Consultoria", CategoryType.INCOME, "#2E6E4E", "briefcase");
        ResponseEntity<CategoryResponse> createResponse = restTemplate.exchange(
            "/api/categories", HttpMethod.POST, new HttpEntity<>(createRequest, user.authHeaders()), CategoryResponse.class
        );
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody().global()).isFalse();
        Long categoryId = createResponse.getBody().id();

        ResponseEntity<CategoryResponse[]> listResponse = restTemplate.exchange(
            "/api/categories", HttpMethod.GET, new HttpEntity<>(user.authHeaders()), CategoryResponse[].class
        );
        assertThat(List.of(listResponse.getBody())).extracting(CategoryResponse::id).contains(categoryId);

        CategoryRequest updateRequest = new CategoryRequest("Consultoria PJ", CategoryType.INCOME, "#2E6E4E", "briefcase");
        ResponseEntity<CategoryResponse> updateResponse = restTemplate.exchange(
            "/api/categories/" + categoryId, HttpMethod.PUT, new HttpEntity<>(updateRequest, user.authHeaders()), CategoryResponse.class
        );
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody().name()).isEqualTo("Consultoria PJ");
    }

    @Test
    void userCannotEditAnotherUsersCategory() {
        TestUser owner = TestDataFactory.registerRandomUser(restTemplate);
        TestUser intruder = TestDataFactory.registerRandomUser(restTemplate);

        CategoryRequest createRequest = new CategoryRequest("Categoria da Ana", CategoryType.EXPENSE, null, null);
        ResponseEntity<CategoryResponse> createResponse = restTemplate.exchange(
            "/api/categories", HttpMethod.POST, new HttpEntity<>(createRequest, owner.authHeaders()), CategoryResponse.class
        );
        Long categoryId = createResponse.getBody().id();

        ResponseEntity<ApiError> updateResponse = restTemplate.exchange(
            "/api/categories/" + categoryId, HttpMethod.PUT,
            new HttpEntity<>(createRequest, intruder.authHeaders()), ApiError.class
        );

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
