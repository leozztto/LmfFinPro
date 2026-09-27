package com.lmf.finpro.integration.calendar;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.RecurrenceFrequency;
import com.lmf.finpro.domain.model.TransactionStatus;
import com.lmf.finpro.infrastructure.web.dto.account.AccountRequest;
import com.lmf.finpro.infrastructure.web.dto.account.AccountResponse;
import com.lmf.finpro.infrastructure.web.dto.calendar.CalendarResponse;
import com.lmf.finpro.infrastructure.web.dto.calendar.CalendarResponse.DayRow;
import com.lmf.finpro.infrastructure.web.dto.calendar.CalendarResponse.EntryRow;
import com.lmf.finpro.infrastructure.web.dto.recurringtransaction.RecurringTransactionRequest;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionRequest;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class CalendarIntegrationTest extends AbstractIntegrationTest {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
    private static final YearMonth NEXT_MONTH = YearMonth.from(TODAY).plusMonths(1);

    @Test
    void combinesPendingForecastsPaidAndOverdue() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user);
        createTransaction(
                user, accountId, "Boleto", "250", NEXT_MONTH.atDay(5), TransactionStatus.PENDING);
        createTransaction(
                user, accountId, "Adiantado", "40", NEXT_MONTH.atDay(7), TransactionStatus.PAID);
        createTransaction(
                user, accountId, "Atrasada", "90", TODAY.minusDays(3), TransactionStatus.PENDING);
        createRecurrence(user, accountId, "Aluguel", "1500", NEXT_MONTH.atDay(10));

        CalendarResponse calendar = get(user, "/api/calendar?month=" + NEXT_MONTH).getBody();

        assertThat(calendar.month()).isEqualTo(NEXT_MONTH);
        assertThat(calendar.today()).isEqualTo(TODAY);
        assertThat(calendar.days())
                .extracting(DayRow::date)
                .containsExactly(NEXT_MONTH.atDay(5), NEXT_MONTH.atDay(10));
        EntryRow forecast = calendar.days().get(1).entries().get(0);
        assertThat(forecast.kind()).isEqualTo("RECURRING_FORECAST");
        assertThat(forecast.status()).isEqualTo("FORECAST");
        assertThat(forecast.accountName()).isEqualTo("Conta");
        assertThat(calendar.expectedExpense()).isEqualByComparingTo("1750");
        assertThat(calendar.paidExpense()).isEqualByComparingTo("40");
        assertThat(calendar.overdue())
                .extracting(EntryRow::description)
                .containsExactly("Atrasada");
        assertThat(calendar.overdueExpense()).isEqualByComparingTo("90");

        CalendarResponse withPaid =
                get(user, "/api/calendar?month=" + NEXT_MONTH + "&includePaid=true").getBody();
        assertThat(withPaid.days())
                .extracting(DayRow::date)
                .containsExactly(NEXT_MONTH.atDay(5), NEXT_MONTH.atDay(7), NEXT_MONTH.atDay(10));
    }

    @Test
    void anotherUserSeesNothingAndFarMonthIsRejected() {
        TestUser owner = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(owner);
        createTransaction(
                owner, accountId, "Boleto", "250", NEXT_MONTH.atDay(5), TransactionStatus.PENDING);

        TestUser other = TestDataFactory.registerRandomUser(restTemplate);
        CalendarResponse calendar = get(other, "/api/calendar?month=" + NEXT_MONTH).getBody();
        assertThat(calendar.days()).isEmpty();
        assertThat(calendar.overdue()).isEmpty();

        assertThat(get(owner, "/api/calendar").getBody().month()).isEqualTo(YearMonth.from(TODAY));
        assertThat(
                        get(owner, "/api/calendar?month=" + YearMonth.from(TODAY).plusYears(6))
                                .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<CalendarResponse> get(TestUser user, String url) {
        return restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(user.authHeaders()), CalendarResponse.class);
    }

    private Long createAccount(TestUser user) {
        return restTemplate
                .exchange(
                        "/api/accounts",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new AccountRequest(
                                        "Conta", AccountType.CHECKING, new BigDecimal("5000")),
                                user.authHeaders()),
                        AccountResponse.class)
                .getBody()
                .id();
    }

    private void createTransaction(
            TestUser user,
            Long accountId,
            String description,
            String amount,
            LocalDate date,
            TransactionStatus status) {
        ResponseEntity<String> response =
                restTemplate.exchange(
                        "/api/transactions",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new TransactionRequest(
                                        accountId,
                                        null,
                                        null,
                                        description,
                                        new BigDecimal(amount),
                                        date,
                                        CategoryType.EXPENSE,
                                        status),
                                user.authHeaders()),
                        String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private void createRecurrence(
            TestUser user, Long accountId, String description, String amount, LocalDate start) {
        ResponseEntity<String> response =
                restTemplate.exchange(
                        "/api/recurring-transactions",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new RecurringTransactionRequest(
                                        accountId,
                                        null,
                                        null,
                                        description,
                                        new BigDecimal(amount),
                                        CategoryType.EXPENSE,
                                        RecurrenceFrequency.MONTHLY,
                                        start,
                                        null),
                                user.authHeaders()),
                        String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
