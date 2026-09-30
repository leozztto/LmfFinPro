package com.lmf.finpro.integration.savingsgoal;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.ContributionType;
import com.lmf.finpro.domain.model.SavingsGoalType;
import com.lmf.finpro.infrastructure.web.dto.account.AccountRequest;
import com.lmf.finpro.infrastructure.web.dto.account.AccountResponse;
import com.lmf.finpro.infrastructure.web.dto.savingsgoal.GoalContributionRequest;
import com.lmf.finpro.infrastructure.web.dto.savingsgoal.GoalContributionResponse;
import com.lmf.finpro.infrastructure.web.dto.savingsgoal.SavingsGoalRequest;
import com.lmf.finpro.infrastructure.web.dto.savingsgoal.SavingsGoalResponse;
import com.lmf.finpro.infrastructure.web.dto.savingsgoal.SavingsGoalUpdateRequest;
import com.lmf.finpro.infrastructure.web.dto.savingsgoal.SuggestedTaxRateResponse;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionRequest;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionResponse;
import com.lmf.finpro.infrastructure.web.dto.transfer.TransferResponse;
import com.lmf.finpro.infrastructure.web.exception.ApiError;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

class SavingsGoalIntegrationTest extends AbstractIntegrationTest {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

    @Test
    void createsGoalRecordsContributionsAndAppliesSuggestion() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long fundingAccountId = createAccount(user, BigDecimal.ZERO);
        Long reserveAccountId = createReserveAccount(user, BigDecimal.ZERO);
        createIncome(user, fundingAccountId, BigDecimal.valueOf(5000), TODAY);

        SavingsGoalResponse goal =
                createGoal(
                        user,
                        new SavingsGoalRequest(
                                "Caixinha do imposto",
                                SavingsGoalType.TAX_RESERVE,
                                BigDecimal.valueOf(10000),
                                null,
                                new BigDecimal("0.06"),
                                false,
                                reserveAccountId,
                                fundingAccountId));
        assertThat(goal.savedAmount()).isEqualByComparingTo("0");
        assertThat(goal.suggestedContribution()).isEqualByComparingTo("300");

        ResponseEntity<GoalContributionResponse> applied =
                restTemplate.exchange(
                        "/api/savings-goals/" + goal.id() + "/contributions/suggested",
                        HttpMethod.POST,
                        new HttpEntity<>(user.authHeaders()),
                        GoalContributionResponse.class);
        assertThat(applied.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(applied.getBody().amount()).isEqualByComparingTo("300");
        assertThat(applied.getBody().transferId()).isNotNull();

        addContribution(user, goal.id(), ContributionType.WITHDRAWAL, BigDecimal.valueOf(50));

        SavingsGoalResponse listed = listGoals(user).get(0);
        assertThat(listed.savedAmount()).isEqualByComparingTo("250");
        assertThat(listed.remainingAmount()).isEqualByComparingTo("9750");
        assertThat(listed.suggestedContribution()).isEqualByComparingTo("0");

        GoalContributionResponse[] history =
                restTemplate
                        .exchange(
                                "/api/savings-goals/" + goal.id() + "/contributions",
                                HttpMethod.GET,
                                new HttpEntity<>(user.authHeaders()),
                                GoalContributionResponse[].class)
                        .getBody();
        assertThat(history).hasSize(2);

        // O aporte/resgate é uma transferência real: aparece na tela de Transferências.
        assertThat(listTransfers(user)).hasSize(2);
    }

    @Test
    void createsGoalWithAutoContributeOnAndPersistsIt() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long fundingAccountId = createAccount(user, BigDecimal.ZERO);
        Long reserveAccountId = createReserveAccount(user, BigDecimal.ZERO);

        SavingsGoalResponse goal =
                createGoal(
                        user,
                        new SavingsGoalRequest(
                                "Caixinha do imposto",
                                SavingsGoalType.TAX_RESERVE,
                                BigDecimal.valueOf(10000),
                                null,
                                new BigDecimal("0.06"),
                                true,
                                reserveAccountId,
                                fundingAccountId));

        assertThat(goal.autoContribute()).isTrue();
        assertThat(listGoals(user).get(0).autoContribute()).isTrue();
    }

    @Test
    void rejectsAutoContributeWithoutIncomeRate() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long fundingAccountId = createAccount(user, BigDecimal.ZERO);
        Long reserveAccountId = createReserveAccount(user, BigDecimal.ZERO);

        ResponseEntity<ApiError> response =
                restTemplate.exchange(
                        "/api/savings-goals",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new SavingsGoalRequest(
                                        "Caixinha",
                                        SavingsGoalType.OTHER,
                                        BigDecimal.valueOf(1000),
                                        null,
                                        null,
                                        true,
                                        reserveAccountId,
                                        fundingAccountId),
                                user.authHeaders()),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createRejectsWhenReserveAndFundingAccountsAreTheSame() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long accountId = createAccount(user, BigDecimal.ZERO);

        ResponseEntity<ApiError> response =
                restTemplate.exchange(
                        "/api/savings-goals",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new SavingsGoalRequest(
                                        "Caixinha",
                                        SavingsGoalType.OTHER,
                                        BigDecimal.valueOf(1000),
                                        null,
                                        null,
                                        false,
                                        accountId,
                                        accountId),
                                user.authHeaders()),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void withdrawalBeyondSavedAmountIsRejected() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long fundingAccountId = createAccount(user, BigDecimal.ZERO);
        Long reserveAccountId = createReserveAccount(user, BigDecimal.ZERO);
        SavingsGoalResponse goal =
                createGoal(
                        user,
                        new SavingsGoalRequest(
                                "Férias",
                                SavingsGoalType.VACATION,
                                BigDecimal.valueOf(3000),
                                TODAY.plusMonths(6),
                                null,
                                false,
                                reserveAccountId,
                                fundingAccountId));

        ResponseEntity<ApiError> response =
                restTemplate.exchange(
                        "/api/savings-goals/" + goal.id() + "/contributions",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new GoalContributionRequest(
                                        ContributionType.WITHDRAWAL, BigDecimal.TEN, TODAY, null),
                                user.authHeaders()),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void anotherUserCannotSeeOrChangeTheGoal() {
        TestUser owner = TestDataFactory.registerRandomUser(restTemplate);
        TestUser other = TestDataFactory.registerRandomUser(restTemplate);
        Long fundingAccountId = createAccount(owner, BigDecimal.ZERO);
        Long reserveAccountId = createReserveAccount(owner, BigDecimal.ZERO);
        SavingsGoalResponse goal =
                createGoal(
                        owner,
                        new SavingsGoalRequest(
                                "Reserva",
                                SavingsGoalType.EMERGENCY_FUND,
                                BigDecimal.valueOf(20000),
                                null,
                                null,
                                false,
                                reserveAccountId,
                                fundingAccountId));

        ResponseEntity<ApiError> response =
                restTemplate.exchange(
                        "/api/savings-goals/" + goal.id(),
                        HttpMethod.DELETE,
                        new HttpEntity<>(other.authHeaders()),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(listGoals(other)).isEmpty();
    }

    /**
     * Excluir uma meta com saldo guardado é bloqueado (409): o dinheiro é real, então só depois do
     * resgate total é que a meta pode ser excluída — sem perder nenhuma transferência já feita.
     */
    @Test
    void deletingAGoalIsBlockedUntilItIsFullyWithdrawn() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        Long fundingAccountId = createAccount(user, BigDecimal.valueOf(1000));
        Long reserveAccountId = createReserveAccount(user, BigDecimal.ZERO);
        SavingsGoalResponse goal =
                createGoal(
                        user,
                        new SavingsGoalRequest(
                                "Férias",
                                SavingsGoalType.VACATION,
                                BigDecimal.valueOf(3000),
                                null,
                                null,
                                false,
                                reserveAccountId,
                                fundingAccountId));
        addContribution(user, goal.id(), ContributionType.DEPOSIT, BigDecimal.valueOf(500));

        ResponseEntity<SavingsGoalResponse> updated =
                restTemplate.exchange(
                        "/api/savings-goals/" + goal.id(),
                        HttpMethod.PUT,
                        new HttpEntity<>(
                                new SavingsGoalUpdateRequest(
                                        "Férias na praia",
                                        SavingsGoalType.VACATION,
                                        BigDecimal.valueOf(4000),
                                        null,
                                        null,
                                        false),
                                user.authHeaders()),
                        SavingsGoalResponse.class);
        assertThat(updated.getBody().name()).isEqualTo("Férias na praia");
        assertThat(updated.getBody().savedAmount()).isEqualByComparingTo("500");

        ResponseEntity<ApiError> blocked =
                restTemplate.exchange(
                        "/api/savings-goals/" + goal.id(),
                        HttpMethod.DELETE,
                        new HttpEntity<>(user.authHeaders()),
                        ApiError.class);
        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        addContribution(user, goal.id(), ContributionType.WITHDRAWAL, BigDecimal.valueOf(500));

        ResponseEntity<Void> deleted =
                restTemplate.exchange(
                        "/api/savings-goals/" + goal.id(),
                        HttpMethod.DELETE,
                        new HttpEntity<>(user.authHeaders()),
                        Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(listGoals(user)).isEmpty();

        // O dinheiro nunca é perdido: as duas transferências (aporte e resgate) continuam no
        // extrato, só deixam de estar rotuladas como de uma meta.
        assertThat(listTransfers(user)).hasSize(2);
    }

    @Test
    void rejectsInvalidGoal() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        ResponseEntity<ApiError> response =
                restTemplate.exchange(
                        "/api/savings-goals",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new SavingsGoalRequest(
                                        " ",
                                        SavingsGoalType.OTHER,
                                        BigDecimal.ZERO,
                                        null,
                                        new BigDecimal("1.5"),
                                        false,
                                        1L,
                                        2L),
                                user.authHeaders()),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void suggestedTaxRateFollowsTheUserRegime() {
        // Usuário de teste é AUTONOMO e ainda não tem receita: faixa isenta.
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        SuggestedTaxRateResponse response =
                restTemplate
                        .exchange(
                                "/api/savings-goals/suggested-tax-rate",
                                HttpMethod.GET,
                                new HttpEntity<>(user.authHeaders()),
                                SuggestedTaxRateResponse.class)
                        .getBody();

        assertThat(response.incomeRate()).isEqualByComparingTo("0");
    }

    private SavingsGoalResponse createGoal(TestUser user, SavingsGoalRequest request) {
        ResponseEntity<SavingsGoalResponse> response =
                restTemplate.exchange(
                        "/api/savings-goals",
                        HttpMethod.POST,
                        new HttpEntity<>(request, user.authHeaders()),
                        SavingsGoalResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private List<SavingsGoalResponse> listGoals(TestUser user) {
        return List.of(
                restTemplate
                        .exchange(
                                "/api/savings-goals",
                                HttpMethod.GET,
                                new HttpEntity<>(user.authHeaders()),
                                SavingsGoalResponse[].class)
                        .getBody());
    }

    private void addContribution(
            TestUser user, Long goalId, ContributionType type, BigDecimal amount) {
        ResponseEntity<GoalContributionResponse> response =
                restTemplate.exchange(
                        "/api/savings-goals/" + goalId + "/contributions",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new GoalContributionRequest(type, amount, TODAY, "Manual"),
                                user.authHeaders()),
                        GoalContributionResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private Long createAccount(TestUser user, BigDecimal initialBalance) {
        return createAccount(user, AccountType.CHECKING, initialBalance);
    }

    private Long createReserveAccount(TestUser user, BigDecimal initialBalance) {
        return createAccount(user, AccountType.RESERVE, initialBalance);
    }

    private Long createAccount(TestUser user, AccountType type, BigDecimal initialBalance) {
        return restTemplate
                .exchange(
                        "/api/accounts",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new AccountRequest("Conta", type, initialBalance),
                                user.authHeaders()),
                        AccountResponse.class)
                .getBody()
                .id();
    }

    private void createIncome(TestUser user, Long accountId, BigDecimal amount, LocalDate date) {
        ResponseEntity<TransactionResponse> response =
                restTemplate.exchange(
                        "/api/transactions",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new TransactionRequest(
                                        accountId,
                                        null,
                                        null,
                                        "Receita",
                                        amount,
                                        date,
                                        CategoryType.INCOME),
                                user.authHeaders()),
                        TransactionResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private List<TransferResponse> listTransfers(TestUser user) {
        return List.of(
                restTemplate
                        .exchange(
                                "/api/transfers",
                                HttpMethod.GET,
                                new HttpEntity<>(user.authHeaders()),
                                TransferResponse[].class)
                        .getBody());
    }
}
