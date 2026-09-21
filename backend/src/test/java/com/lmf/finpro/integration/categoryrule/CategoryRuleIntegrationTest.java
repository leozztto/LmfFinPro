package com.lmf.finpro.integration.categoryrule;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.infrastructure.web.dto.category.CategoryRequest;
import com.lmf.finpro.infrastructure.web.dto.category.CategoryResponse;
import com.lmf.finpro.infrastructure.web.dto.categoryrule.CategoryRuleRequest;
import com.lmf.finpro.infrastructure.web.dto.categoryrule.CategoryRuleResponse;
import com.lmf.finpro.infrastructure.web.exception.ApiError;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

class CategoryRuleIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createsListsAndDeletesOwnRule() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long categoryId = createCategory(user);

        CategoryRuleRequest createRequest = new CategoryRuleRequest("UBER", categoryId);
        ResponseEntity<CategoryRuleResponse> createResponse =
                restTemplate.exchange(
                        "/api/category-rules",
                        HttpMethod.POST,
                        new HttpEntity<>(createRequest, user.authHeaders()),
                        CategoryRuleResponse.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long ruleId = createResponse.getBody().id();

        ResponseEntity<CategoryRuleResponse[]> listResponse =
                restTemplate.exchange(
                        "/api/category-rules",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        CategoryRuleResponse[].class);
        assertThat(List.of(listResponse.getBody()))
                .extracting(CategoryRuleResponse::id)
                .contains(ruleId);

        ResponseEntity<Void> deleteResponse =
                restTemplate.exchange(
                        "/api/category-rules/" + ruleId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(user.authHeaders()),
                        Void.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void userCannotDeleteAnotherUsersRule() {
        TestUser owner = TestDataFactory.registerRandomUser(restTemplate);
        TestUser intruder = TestDataFactory.registerRandomUser(restTemplate);
        Long categoryId = createCategory(owner);

        CategoryRuleRequest createRequest = new CategoryRuleRequest("MERCADO", categoryId);
        ResponseEntity<CategoryRuleResponse> createResponse =
                restTemplate.exchange(
                        "/api/category-rules",
                        HttpMethod.POST,
                        new HttpEntity<>(createRequest, owner.authHeaders()),
                        CategoryRuleResponse.class);
        Long ruleId = createResponse.getBody().id();

        ResponseEntity<ApiError> deleteResponse =
                restTemplate.exchange(
                        "/api/category-rules/" + ruleId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(intruder.authHeaders()),
                        ApiError.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectsRuleForCategoryNotOwned() {
        TestUser owner = TestDataFactory.registerRandomUser(restTemplate);
        TestUser intruder = TestDataFactory.registerRandomUser(restTemplate);
        Long categoryId = createCategory(owner);

        CategoryRuleRequest createRequest = new CategoryRuleRequest("ALUGUEL", categoryId);
        ResponseEntity<ApiError> response =
                restTemplate.exchange(
                        "/api/category-rules",
                        HttpMethod.POST,
                        new HttpEntity<>(createRequest, intruder.authHeaders()),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Long createCategory(TestUser user) {
        CategoryRequest request =
                new CategoryRequest("Categoria para regra", CategoryType.EXPENSE, null, null);
        ResponseEntity<CategoryResponse> response =
                restTemplate.exchange(
                        "/api/categories",
                        HttpMethod.POST,
                        new HttpEntity<>(request, user.authHeaders()),
                        CategoryResponse.class);
        return response.getBody().id();
    }
}
