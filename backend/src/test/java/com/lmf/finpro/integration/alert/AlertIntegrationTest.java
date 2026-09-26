package com.lmf.finpro.integration.alert;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.AlertDigest;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.port.out.AlertMailerPort;
import com.lmf.finpro.infrastructure.scheduling.AlertScheduler;
import com.lmf.finpro.infrastructure.web.dto.account.AccountRequest;
import com.lmf.finpro.infrastructure.web.dto.account.AccountResponse;
import com.lmf.finpro.infrastructure.web.dto.budget.BudgetRequest;
import com.lmf.finpro.infrastructure.web.dto.category.CategoryRequest;
import com.lmf.finpro.infrastructure.web.dto.category.CategoryResponse;
import com.lmf.finpro.infrastructure.web.dto.profile.NotificationPreferencesRequest;
import com.lmf.finpro.infrastructure.web.dto.profile.NotificationPreferencesResponse;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionRequest;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionResponse;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

class AlertIntegrationTest extends AbstractIntegrationTest {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

    static class CapturingAlertMailer implements AlertMailerPort {
        final Map<String, List<AlertDigest>> digestsByEmail = new ConcurrentHashMap<>();

        @Override
        public void sendDigest(String toEmail, String userName, AlertDigest digest) {
            digestsByEmail
                    .computeIfAbsent(toEmail, email -> new CopyOnWriteArrayList<>())
                    .add(digest);
        }
    }

    @TestConfiguration
    static class MailerConfig {
        @Bean
        @Primary
        CapturingAlertMailer capturingAlertMailer() {
            return new CapturingAlertMailer();
        }
    }

    @Autowired private CapturingAlertMailer mailer;
    @Autowired private AlertScheduler alertScheduler;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void sendsDigestOnceWithBillDueAndBudgetOverLimit() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);
        Long categoryId = createCategory(user);
        createBudget(user, categoryId, BigDecimal.valueOf(500));
        createExpense(user, accountId, categoryId, BigDecimal.valueOf(600), TODAY);
        createExpense(user, accountId, null, BigDecimal.valueOf(120), TODAY.plusDays(1));

        alertScheduler.sendDailyAlerts();
        alertScheduler.sendDailyAlerts();

        List<AlertDigest> digests = mailer.digestsByEmail.get(user.email());
        assertThat(digests).hasSize(1);
        AlertDigest digest = digests.get(0);
        assertThat(digest.bills())
                .singleElement()
                .extracting("dueDate")
                .isEqualTo(TODAY.plusDays(1));
        assertThat(digest.budgets()).singleElement().extracting("threshold").isEqualTo(100);
    }

    @Test
    void userWithUnreadableRegisterDoesNotBlockTheOthers() {
        TestUser broken = TestDataFactory.registerRandomUser(restTemplate);
        // Valor fora do enum TaxRegime, como os gravados direto no banco: ler esse usuário falha.
        jdbcTemplate.update(
                "UPDATE users SET tax_regime = 'SIMPLES' WHERE id = ?", broken.userId());
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);
        createExpense(user, accountId, null, BigDecimal.valueOf(120), TODAY.plusDays(1));

        try {
            alertScheduler.sendDailyAlerts();
        } finally {
            jdbcTemplate.update(
                    "UPDATE users SET tax_regime = 'AUTONOMO' WHERE id = ?", broken.userId());
        }

        assertThat(mailer.digestsByEmail.get(user.email())).hasSize(1);
    }

    @Test
    void userWithBillsDisabledReceivesNothing() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);
        createExpense(user, accountId, null, BigDecimal.valueOf(120), TODAY.plusDays(1));
        putPreferences(user, new NotificationPreferencesRequest(false, 3, true, true));

        alertScheduler.sendDailyAlerts();

        assertThat(mailer.digestsByEmail.get(user.email())).isNull();
    }

    @Test
    void preferencesStartWithDefaultsAndCanBeUpdated() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        NotificationPreferencesResponse defaults = getPreferences(user);
        assertThat(defaults).isEqualTo(new NotificationPreferencesResponse(true, 3, true, true));

        ResponseEntity<NotificationPreferencesResponse> updated =
                putPreferences(user, new NotificationPreferencesRequest(true, 7, false, true));
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getPreferences(user))
                .isEqualTo(new NotificationPreferencesResponse(true, 7, false, true));
    }

    @Test
    void preferencesRejectDaysBeforeOutOfRange() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        ResponseEntity<NotificationPreferencesResponse> response =
                putPreferences(user, new NotificationPreferencesRequest(true, 16, true, true));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private NotificationPreferencesResponse getPreferences(TestUser user) {
        return restTemplate
                .exchange(
                        "/api/profile/notifications",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        NotificationPreferencesResponse.class)
                .getBody();
    }

    private ResponseEntity<NotificationPreferencesResponse> putPreferences(
            TestUser user, NotificationPreferencesRequest request) {
        return restTemplate.exchange(
                "/api/profile/notifications",
                HttpMethod.PUT,
                new HttpEntity<>(request, user.authHeaders()),
                NotificationPreferencesResponse.class);
    }

    private Long createAccount(TestUser user) {
        return restTemplate
                .exchange(
                        "/api/accounts",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new AccountRequest(
                                        "Conta", AccountType.CHECKING, BigDecimal.valueOf(1000)),
                                user.authHeaders()),
                        AccountResponse.class)
                .getBody()
                .id();
    }

    private Long createCategory(TestUser user) {
        return restTemplate
                .exchange(
                        "/api/categories",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new CategoryRequest("Mercado", CategoryType.EXPENSE, null, null),
                                user.authHeaders()),
                        CategoryResponse.class)
                .getBody()
                .id();
    }

    private void createBudget(TestUser user, Long categoryId, BigDecimal limit) {
        ResponseEntity<Void> response =
                restTemplate.exchange(
                        "/api/budgets",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new BudgetRequest(categoryId, YearMonth.from(TODAY), limit),
                                user.authHeaders()),
                        Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private void createExpense(
            TestUser user, Long accountId, Long categoryId, BigDecimal amount, LocalDate date) {
        ResponseEntity<TransactionResponse> response =
                restTemplate.exchange(
                        "/api/transactions",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new TransactionRequest(
                                        accountId,
                                        categoryId,
                                        null,
                                        "Despesa",
                                        amount,
                                        date,
                                        CategoryType.EXPENSE),
                                user.authHeaders()),
                        TransactionResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
