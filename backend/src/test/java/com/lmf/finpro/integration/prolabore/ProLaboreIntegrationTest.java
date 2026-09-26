package com.lmf.finpro.integration.prolabore;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.AccountScope;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.ProLaboreCalculationBase;
import com.lmf.finpro.domain.model.ProLaboreTaxMode;
import com.lmf.finpro.domain.model.ProLaboreWithholdingMode;
import com.lmf.finpro.infrastructure.web.dto.account.AccountRequest;
import com.lmf.finpro.infrastructure.web.dto.account.AccountResponse;
import com.lmf.finpro.infrastructure.web.dto.prolabore.ProLaboreResponse;
import com.lmf.finpro.infrastructure.web.dto.prolabore.ProLaboreSettingsRequest;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionRequest;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionResponse;
import com.lmf.finpro.infrastructure.web.dto.transfer.TransferRequest;
import com.lmf.finpro.infrastructure.web.dto.transfer.TransferResponse;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

class ProLaboreIntegrationTest extends AbstractIntegrationTest {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

    @Test
    void accountsAreCreatedAsPersonalUnlessMarkedAsBusiness() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        AccountResponse personal = createAccount(user, "Pessoal", BigDecimal.ZERO, null);
        AccountResponse business =
                createAccount(user, "Empresa", BigDecimal.ZERO, AccountScope.BUSINESS);

        assertThat(personal.scope()).isEqualTo(AccountScope.PERSONAL);
        assertThat(business.scope()).isEqualTo(AccountScope.BUSINESS);

        // Editar sem informar o uso mantém o atual.
        AccountResponse renamed =
                restTemplate
                        .exchange(
                                "/api/accounts/" + business.id(),
                                HttpMethod.PUT,
                                new HttpEntity<>(
                                        new AccountRequest(
                                                "Empresa renomeada",
                                                AccountType.CHECKING,
                                                BigDecimal.ZERO),
                                        user.authHeaders()),
                                AccountResponse.class)
                        .getBody();
        assertThat(renamed.scope()).isEqualTo(AccountScope.BUSINESS);
    }

    @Test
    void summaryShowsAvailableAndTransfersFromBusinessToPersonalAsWithdrawn() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        AccountResponse business =
                createAccount(user, "Empresa", BigDecimal.valueOf(10000), AccountScope.BUSINESS);
        AccountResponse personal =
                createAccount(user, "Pessoal", BigDecimal.ZERO, AccountScope.PERSONAL);
        ResponseEntity<TransferResponse> transfer =
                restTemplate.exchange(
                        "/api/transfers",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new TransferRequest(
                                        business.id(),
                                        personal.id(),
                                        BigDecimal.valueOf(2000),
                                        TODAY,
                                        "Pró-labore"),
                                user.authHeaders()),
                        TransferResponse.class);
        assertThat(transfer.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        createIncome(user, business.id(), BigDecimal.valueOf(5000));

        ProLaboreResponse summary = getSummary(user);

        // Base padrão (receitas do mês), usuário AUTONOMO: 5000 na faixa de 27,5% = 1375 de
        // imposto; reserva de 10% = 500; já retirado 2000 → 5000 − 1375 − 500 − 2000 = 1125.
        assertThat(summary.hasBusinessAccounts()).isTrue();
        assertThat(summary.settings().calculationBase()).isEqualTo("MONTH_INCOME");
        assertThat(summary.businessBalance()).isEqualByComparingTo("13000");
        assertThat(summary.monthBusinessIncome()).isEqualByComparingTo("5000");
        assertThat(summary.taxReserve()).isEqualByComparingTo("1375");
        assertThat(summary.reserve()).isEqualByComparingTo("500");
        assertThat(summary.withdrawnThisMonth()).isEqualByComparingTo("2000");
        assertThat(summary.withdrawals()).hasSize(1);
        assertThat(summary.availableToWithdraw()).isEqualByComparingTo("1125");
        assertThat(summary.suggestedFromAccountId()).isEqualTo(business.id());
        assertThat(summary.suggestedToAccountId()).isEqualTo(personal.id());

        // Na base de saldo: 13000 − 1375 de imposto − colchão (sem despesas, zero).
        ProLaboreResponse byBalance =
                putSettings(
                                user,
                                new ProLaboreSettingsRequest(
                                        ProLaboreCalculationBase.CURRENT_BALANCE,
                                        1,
                                        new BigDecimal("0.10"),
                                        ProLaboreTaxMode.AUTOMATIC,
                                        null,
                                        null))
                        .getBody();
        assertThat(byBalance.availableToWithdraw()).isEqualByComparingTo("11625");
    }

    @Test
    void fixedAmountShowsWhatIsLeftAndWhetherItIsCovered() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        AccountResponse business =
                createAccount(user, "Empresa", BigDecimal.ZERO, AccountScope.BUSINESS);
        createAccount(user, "Pessoal", BigDecimal.ZERO, AccountScope.PERSONAL);
        createIncome(user, business.id(), BigDecimal.valueOf(10000));

        // Imposto manual de 10% e sem reserva: disponível = 10000 − 1000 = 9000.
        ProLaboreResponse summary =
                putSettings(
                                user,
                                new ProLaboreSettingsRequest(
                                        ProLaboreCalculationBase.MONTH_INCOME,
                                        1,
                                        BigDecimal.ZERO,
                                        ProLaboreTaxMode.MANUAL,
                                        new BigDecimal("0.10"),
                                        BigDecimal.valueOf(6000)))
                        .getBody();

        assertThat(summary.availableToWithdraw()).isEqualByComparingTo("9000");
        assertThat(summary.fixedRemaining()).isEqualByComparingTo("6000");
        assertThat(summary.fixedCovered()).isTrue();
        assertThat(summary.suggestedPayment()).isEqualByComparingTo("6000");
    }

    @Test
    void enabledWithholdingDeductsInssAndIrrfFromTheProLabore() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        AccountResponse business =
                createAccount(user, "Empresa", BigDecimal.ZERO, AccountScope.BUSINESS);
        createAccount(user, "Pessoal", BigDecimal.ZERO, AccountScope.PERSONAL);
        createIncome(user, business.id(), BigDecimal.valueOf(5000));

        ProLaboreResponse summary =
                putSettings(
                                user,
                                new ProLaboreSettingsRequest(
                                        ProLaboreCalculationBase.MONTH_INCOME,
                                        1,
                                        new BigDecimal("0.10"),
                                        ProLaboreTaxMode.AUTOMATIC,
                                        null,
                                        null,
                                        ProLaboreWithholdingMode.ENABLED,
                                        BigDecimal.ZERO))
                        .getBody();

        // Orçamento 5000 − 1375 (27,5%, autônomo) − 500 = 3125 de bruto; INSS 11% = 343,75;
        // IRRF zero (até R$ 5.000); líquido 2781,25.
        assertThat(summary.withholdingApplied()).isTrue();
        assertThat(summary.monthBudget()).isEqualByComparingTo("3125");
        assertThat(summary.payroll().gross()).isEqualByComparingTo("3125");
        assertThat(summary.payroll().employeeInss()).isEqualByComparingTo("343.75");
        assertThat(summary.payroll().irrf()).isEqualByComparingTo("0");
        assertThat(summary.payroll().net()).isEqualByComparingTo("2781.25");
        assertThat(summary.availableToWithdraw()).isEqualByComparingTo("2781.25");
        assertThat(summary.settings().withholdingMode()).isEqualTo("ENABLED");
    }

    @Test
    void userWithoutBusinessAccountsHasNothingToWithdraw() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        createAccount(user, "Pessoal", BigDecimal.valueOf(5000), null);

        ProLaboreResponse summary = getSummary(user);

        assertThat(summary.hasBusinessAccounts()).isFalse();
        assertThat(summary.availableToWithdraw()).isEqualByComparingTo("0");
    }

    @Test
    void settingsStartWithDefaultsPersistAndRejectInvalidValues() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        ProLaboreResponse.SettingsResponse defaults = getSummary(user).settings();
        assertThat(defaults.calculationBase()).isEqualTo("MONTH_INCOME");
        assertThat(defaults.reserveRate()).isEqualByComparingTo("0.10");
        assertThat(defaults.taxMode()).isEqualTo("AUTOMATIC");
        assertThat(defaults.fixedAmount()).isNull();

        ResponseEntity<ProLaboreResponse> updated =
                putSettings(
                        user,
                        new ProLaboreSettingsRequest(
                                ProLaboreCalculationBase.CURRENT_BALANCE,
                                3,
                                new BigDecimal("0.20"),
                                ProLaboreTaxMode.MANUAL,
                                new BigDecimal("0.08"),
                                BigDecimal.valueOf(4000)));
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        ProLaboreResponse.SettingsResponse saved = getSummary(user).settings();
        assertThat(saved.calculationBase()).isEqualTo("CURRENT_BALANCE");
        assertThat(saved.cashCushionMonths()).isEqualTo(3);
        assertThat(saved.reserveRate()).isEqualByComparingTo("0.20");
        assertThat(saved.manualTaxRate()).isEqualByComparingTo("0.08");
        assertThat(saved.fixedAmount()).isEqualByComparingTo("4000");

        assertThat(
                        putSettings(
                                        user,
                                        new ProLaboreSettingsRequest(
                                                ProLaboreCalculationBase.MONTH_INCOME,
                                                13,
                                                new BigDecimal("0.10"),
                                                ProLaboreTaxMode.AUTOMATIC,
                                                null,
                                                null))
                                .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        // Modo manual sem alíquota.
        assertThat(
                        putSettings(
                                        user,
                                        new ProLaboreSettingsRequest(
                                                ProLaboreCalculationBase.MONTH_INCOME,
                                                1,
                                                new BigDecimal("0.10"),
                                                ProLaboreTaxMode.MANUAL,
                                                null,
                                                null))
                                .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private ProLaboreResponse getSummary(TestUser user) {
        return restTemplate
                .exchange(
                        "/api/pro-labore",
                        HttpMethod.GET,
                        new HttpEntity<>(user.authHeaders()),
                        ProLaboreResponse.class)
                .getBody();
    }

    private ResponseEntity<ProLaboreResponse> putSettings(
            TestUser user, ProLaboreSettingsRequest request) {
        return restTemplate.exchange(
                "/api/pro-labore/settings",
                HttpMethod.PUT,
                new HttpEntity<>(request, user.authHeaders()),
                ProLaboreResponse.class);
    }

    private void createIncome(TestUser user, Long accountId, BigDecimal amount) {
        ResponseEntity<TransactionResponse> response =
                restTemplate.exchange(
                        "/api/transactions",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new TransactionRequest(
                                        accountId,
                                        null,
                                        null,
                                        "Receita PJ",
                                        amount,
                                        TODAY,
                                        CategoryType.INCOME),
                                user.authHeaders()),
                        TransactionResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private AccountResponse createAccount(
            TestUser user, String name, BigDecimal initialBalance, AccountScope scope) {
        ResponseEntity<AccountResponse> response =
                restTemplate.exchange(
                        "/api/accounts",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new AccountRequest(
                                        name, AccountType.CHECKING, initialBalance, scope),
                                user.authHeaders()),
                        AccountResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }
}
