package com.lmf.finpro.integration.household;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.ClientWorkType;
import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.port.out.HouseholdInviteMailerPort;
import com.lmf.finpro.infrastructure.security.JwtAuthenticationFilter;
import com.lmf.finpro.infrastructure.web.dto.account.AccountRequest;
import com.lmf.finpro.infrastructure.web.dto.account.AccountResponse;
import com.lmf.finpro.infrastructure.web.dto.category.CategoryRequest;
import com.lmf.finpro.infrastructure.web.dto.category.CategoryResponse;
import com.lmf.finpro.infrastructure.web.dto.client.ClientRequest;
import com.lmf.finpro.infrastructure.web.dto.client.ClientResponse;
import com.lmf.finpro.infrastructure.web.dto.common.PageResponse;
import com.lmf.finpro.infrastructure.web.dto.household.AcceptInviteRequest;
import com.lmf.finpro.infrastructure.web.dto.household.AccountSharingResponse;
import com.lmf.finpro.infrastructure.web.dto.household.HouseholdRequest;
import com.lmf.finpro.infrastructure.web.dto.household.HouseholdResponse;
import com.lmf.finpro.infrastructure.web.dto.household.InviteRequest;
import com.lmf.finpro.infrastructure.web.dto.household.ShareAccountsRequest;
import com.lmf.finpro.infrastructure.web.dto.tag.TagResponse;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionRequest;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionResponse;
import com.lmf.finpro.infrastructure.web.dto.transfer.LinkableAccountResponse;
import com.lmf.finpro.infrastructure.web.dto.transfer.TransferRequest;
import com.lmf.finpro.infrastructure.web.dto.transfer.TransferResponse;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.CpfTestFactory;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Compartilhar contas do espaço pessoal com um grupo: as contas e o histórico mudam de dono, e o
 * que as transações usam (categoria, cliente, tag) passa a existir no grupo.
 */
class AccountSharingIntegrationTest extends AbstractIntegrationTest {

    static class CapturingInviteMailer implements HouseholdInviteMailerPort {
        final Map<String, String> tokenByEmail = new ConcurrentHashMap<>();

        @Override
        public void sendInvite(
                String toEmail,
                String inviterName,
                String householdName,
                String inviteLink,
                long ttlDays) {
            tokenByEmail.put(toEmail, inviteLink.substring(inviteLink.indexOf("token=") + 6));
        }
    }

    @TestConfiguration
    static class MailerConfig {
        @Bean
        @Primary
        CapturingInviteMailer capturingInviteMailer() {
            return new CapturingInviteMailer();
        }
    }

    @Autowired private CapturingInviteMailer mailer;

    private HttpHeaders headers(TestUser user, Long householdId) {
        HttpHeaders headers = user.authHeaders();
        if (householdId != null) {
            headers.add(JwtAuthenticationFilter.HOUSEHOLD_HEADER, String.valueOf(householdId));
        }
        return headers;
    }

    private <T> ResponseEntity<T> post(
            TestUser user, Long householdId, String url, Object body, Class<T> type) {
        return restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers(user, householdId)), type);
    }

    private <T> ResponseEntity<T> get(TestUser user, Long householdId, String url, Class<T> type) {
        return restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers(user, householdId)), type);
    }

    private Long createShared(TestUser owner) {
        return post(
                        owner,
                        null,
                        "/api/households",
                        new HouseholdRequest("Família Silva"),
                        HouseholdResponse.class)
                .getBody()
                .id();
    }

    private void join(TestUser owner, Long householdId, TestUser member) {
        post(
                owner,
                null,
                "/api/households/" + householdId + "/invites",
                new InviteRequest(member.email()),
                String.class);
        post(
                member,
                null,
                "/api/households/invites/accept",
                new AcceptInviteRequest(mailer.tokenByEmail.get(member.email())),
                HouseholdResponse.class);
    }

    private Long createAccount(TestUser user, String name) {
        return createAccount(user, name, AccountType.CHECKING);
    }

    private Long createAccount(TestUser user, String name, AccountType type) {
        return post(
                        user,
                        null,
                        "/api/accounts",
                        new AccountRequest(name, type, BigDecimal.valueOf(1000)),
                        AccountResponse.class)
                .getBody()
                .id();
    }

    private ResponseEntity<AccountSharingResponse> share(
            TestUser user, Long householdId, Long... accountIds) {
        return post(
                user,
                null,
                "/api/households/" + householdId + "/accounts/share",
                new ShareAccountsRequest(Set.of(accountIds)),
                AccountSharingResponse.class);
    }

    private List<Long> accountIds(TestUser user, Long householdId) {
        return List.of(get(user, householdId, "/api/accounts", AccountResponse[].class).getBody())
                .stream()
                .map(AccountResponse::id)
                .toList();
    }

    private List<TransactionResponse> transactions(TestUser user, Long householdId) {
        return restTemplate
                .exchange(
                        "/api/transactions?size=100",
                        HttpMethod.GET,
                        new HttpEntity<>(headers(user, householdId)),
                        new ParameterizedTypeReference<PageResponse<TransactionResponse>>() {})
                .getBody()
                .content();
    }

    @Test
    void sharingMovesTheAccountWithItsHistoryAndRecreatesWhatItUses() {
        TestUser ana = TestDataFactory.registerRandomUser(restTemplate);
        TestUser bia = TestDataFactory.registerRandomUser(restTemplate);
        Long sharedId = createShared(ana);
        join(ana, sharedId, bia);

        Long accountId = createAccount(ana, "Conta corrente");
        Long categoryId =
                post(
                                ana,
                                null,
                                "/api/categories",
                                new CategoryRequest(
                                        "Mercado", CategoryType.EXPENSE, "#112233", null),
                                CategoryResponse.class)
                        .getBody()
                        .id();
        Long clientId =
                post(
                                ana,
                                null,
                                "/api/clients",
                                new ClientRequest(
                                        "Acme",
                                        "acme@finpro.test",
                                        "11987654321",
                                        DocumentType.CPF,
                                        CpfTestFactory.randomValidCpf(),
                                        ClientWorkType.AUTONOMO,
                                        null,
                                        null,
                                        true),
                                ClientResponse.class)
                        .getBody()
                        .id();
        Long transactionId =
                post(
                                ana,
                                null,
                                "/api/transactions",
                                new TransactionRequest(
                                        accountId,
                                        categoryId,
                                        clientId,
                                        "Compra do mês",
                                        BigDecimal.valueOf(50),
                                        LocalDate.now(),
                                        CategoryType.EXPENSE,
                                        null,
                                        List.of("viagem")),
                                TransactionResponse.class)
                        .getBody()
                        .id();

        ResponseEntity<AccountSharingResponse> shared = share(ana, sharedId, accountId);

        assertThat(shared.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(shared.getBody()).isEqualTo(new AccountSharingResponse(1, 1));
        // Mudou de dono: some do espaço pessoal da Ana e aparece para a Bia no grupo.
        assertThat(accountIds(ana, null)).doesNotContain(accountId);
        assertThat(accountIds(bia, sharedId)).contains(accountId);
        assertThat(transactions(ana, null))
                .extracting(TransactionResponse::id)
                .doesNotContain(transactionId);
        TransactionResponse moved =
                transactions(bia, sharedId).stream()
                        .filter(t -> t.id().equals(transactionId))
                        .findFirst()
                        .orElseThrow();
        // A categoria, o cliente e a tag agora existem no grupo (cópias), e a transação aponta para
        // elas.
        CategoryResponse[] sharedCategories =
                get(bia, sharedId, "/api/categories", CategoryResponse[].class).getBody();
        assertThat(List.of(sharedCategories))
                .filteredOn(c -> c.id().equals(moved.categoryId()))
                .singleElement()
                .satisfies(c -> assertThat(c.name()).isEqualTo("Mercado"));
        assertThat(moved.categoryId()).isNotEqualTo(categoryId);
        ClientResponse[] sharedClients =
                get(bia, sharedId, "/api/clients", ClientResponse[].class).getBody();
        assertThat(List.of(sharedClients))
                .filteredOn(c -> c.id().equals(moved.clientId()))
                .singleElement()
                .satisfies(c -> assertThat(c.name()).isEqualTo("Acme"));
        TagResponse[] sharedTags = get(bia, sharedId, "/api/tags", TagResponse[].class).getBody();
        assertThat(List.of(sharedTags)).extracting(TagResponse::name).contains("viagem");
        // A categoria original continua no espaço pessoal da Ana (orçamentos dela não mudam).
        CategoryResponse[] personalCategories =
                get(ana, null, "/api/categories", CategoryResponse[].class).getBody();
        assertThat(List.of(personalCategories))
                .extracting(CategoryResponse::id)
                .contains(categoryId);
    }

    @Test
    void aTransferToAnAccountLeftBehindIsSplitBetweenTheTwoSpaces() {
        TestUser ana = TestDataFactory.registerRandomUser(restTemplate);
        TestUser bia = TestDataFactory.registerRandomUser(restTemplate);
        Long sharedId = createShared(ana);
        join(ana, sharedId, bia);
        Long conjunta = createAccount(ana, "Conjunta");
        Long pessoal = createAccount(ana, "Pessoal");
        // Uma com descrição digitada e uma com a descrição automática (que cita o nome da conta).
        post(
                ana,
                null,
                "/api/transfers",
                new TransferRequest(
                        pessoal, conjunta, BigDecimal.valueOf(100), LocalDate.now(), "Reserva"),
                TransferResponse.class);
        post(
                ana,
                null,
                "/api/transfers",
                new TransferRequest(
                        pessoal, conjunta, BigDecimal.valueOf(50), LocalDate.now(), null),
                TransferResponse.class);

        // Só a conta conjunta vai para o grupo: a pessoal não precisa ir junto.
        assertThat(share(ana, sharedId, conjunta).getStatusCode()).isEqualTo(HttpStatus.OK);

        // O grupo enxerga as duas entradas na conjunta e o nome (só o nome) da conta de origem.
        List<TransactionResponse> inGroup = transactions(bia, sharedId);
        assertThat(inGroup)
                .hasSize(2)
                .allSatisfy(t -> assertThat(t.accountId()).isEqualTo(conjunta));
        assertThat(inGroup)
                .extracting(TransactionResponse::description)
                .contains("Reserva", "Transferência de Pessoal");
        assertThat(accountIds(bia, sharedId)).containsExactly(conjunta);
        assertThat(get(bia, sharedId, "/api/transfers", TransferResponse[].class).getBody())
                .hasSize(2);
        assertThat(get(bia, sharedId, "/api/transfers", TransferResponse[].class).getBody())
                .allSatisfy(
                        transfer -> {
                            assertThat(transfer.fromAccountName()).isEqualTo("Pessoal");
                            assertThat(transfer.toAccountName()).isEqualTo("Conjunta");
                        });
        // O saldo do grupo conta a entrada das transferências (1000 inicial + 100 + 50), e o
        // pessoal a saída.
        assertThat(currentBalance(bia, sharedId)).isEqualTo(1150.0);
        assertThat(currentBalance(ana, null)).isEqualTo(850.0);

        // A pessoa continua vendo, no espaço pessoal, a saída da conta pessoal e também a entrada
        // na conjunta (que está em outro espaço do qual ela participa), com o nome da conta.
        assertThat(accountIds(ana, null)).containsExactly(pessoal);
        List<TransactionResponse> personal = transactions(ana, null);
        assertThat(personal).hasSize(4);
        assertThat(personal)
                .filteredOn(t -> t.accountId().equals(pessoal))
                .hasSize(2)
                .allSatisfy(t -> assertThat(t.type()).isEqualTo(CategoryType.EXPENSE));
        assertThat(personal)
                .filteredOn(t -> t.accountId().equals(conjunta))
                .hasSize(2)
                .allSatisfy(
                        t -> {
                            assertThat(t.type()).isEqualTo(CategoryType.INCOME);
                            assertThat(t.linkedAccountName()).isEqualTo("Conjunta");
                        });
        assertThat(get(ana, null, "/api/transfers", TransferResponse[].class).getBody()).hasSize(2);

        // Compartilhando a outra conta depois, as metades se juntam de novo no grupo.
        assertThat(share(ana, sharedId, pessoal).getStatusCode()).isEqualTo(HttpStatus.OK);
        TransferResponse[] merged =
                get(ana, sharedId, "/api/transfers", TransferResponse[].class).getBody();
        assertThat(List.of(merged)).hasSize(2);
        assertThat(get(ana, null, "/api/transfers", TransferResponse[].class).getBody()).isEmpty();
        assertThat(transactions(bia, sharedId)).hasSize(4);
    }

    private double monthFlow(TestUser user, Long householdId, String field) {
        Map<?, ?> overview = get(user, householdId, "/api/dashboard/overview", Map.class).getBody();
        return ((Number) overview.get(field)).doubleValue();
    }

    private double currentBalance(TestUser user, Long householdId) {
        Map<?, ?> overview = get(user, householdId, "/api/dashboard/overview", Map.class).getBody();
        return ((Number) overview.get("currentBalance")).doubleValue();
    }

    private ResponseEntity<AccountSharingResponse> unshare(
            TestUser user, Long householdId, Long... accountIds) {
        return post(
                user,
                null,
                "/api/households/" + householdId + "/accounts/unshare",
                new ShareAccountsRequest(Set.of(accountIds)),
                AccountSharingResponse.class);
    }

    @Test
    void theOwnerCanUnshareAnAccountAndItComesBackWithItsHistory() {
        TestUser ana = TestDataFactory.registerRandomUser(restTemplate);
        TestUser bia = TestDataFactory.registerRandomUser(restTemplate);
        Long sharedId = createShared(ana);
        join(ana, sharedId, bia);
        Long conjunta = createAccount(ana, "Conjunta");
        Long transactionId = createTransactionIn(ana, null, conjunta);
        share(ana, sharedId, conjunta);
        assertThat(accountIds(bia, sharedId)).contains(conjunta);

        ResponseEntity<AccountSharingResponse> unshared = unshare(ana, sharedId, conjunta);

        assertThat(unshared.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(unshared.getBody()).isEqualTo(new AccountSharingResponse(1, 1));
        assertThat(accountIds(ana, null)).contains(conjunta);
        assertThat(transactions(ana, null))
                .extracting(TransactionResponse::id)
                .contains(transactionId);
        assertThat(accountIds(bia, sharedId)).doesNotContain(conjunta);
        assertThat(transactions(bia, sharedId))
                .extracting(TransactionResponse::id)
                .doesNotContain(transactionId);
    }

    @Test
    void onlyWhoBroughtTheAccountCanUnshareIt() {
        TestUser ana = TestDataFactory.registerRandomUser(restTemplate);
        TestUser bia = TestDataFactory.registerRandomUser(restTemplate);
        Long sharedId = createShared(ana);
        join(ana, sharedId, bia);
        Long byAna = createAccount(ana, "Da Ana");
        Long byBia = createAccount(bia, "Da Bia");
        share(ana, sharedId, byAna);
        share(bia, sharedId, byBia);

        // Nem a outra pessoa nem o dono do grupo levam embora a conta que alguém trouxe.
        assertThat(unshare(bia, sharedId, byAna).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(unshare(ana, sharedId, byBia).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(accountIds(bia, sharedId)).contains(byAna, byBia);
        assertThat(unshare(bia, sharedId, byBia).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(accountIds(bia, null)).contains(byBia);
    }

    @Test
    void theListTellsWhoBroughtEachAccountAndWhoCanUnshareIt() {
        TestUser ana = TestDataFactory.registerRandomUser(restTemplate);
        TestUser bia = TestDataFactory.registerRandomUser(restTemplate);
        Long sharedId = createShared(ana);
        join(ana, sharedId, bia);
        Long conjunta = createAccount(ana, "Conjunta");
        share(ana, sharedId, conjunta);

        AccountResponse forAna =
                List.of(get(ana, sharedId, "/api/accounts", AccountResponse[].class).getBody())
                        .stream()
                        .filter(a -> a.id().equals(conjunta))
                        .findFirst()
                        .orElseThrow();
        AccountResponse forBia =
                List.of(get(bia, sharedId, "/api/accounts", AccountResponse[].class).getBody())
                        .stream()
                        .filter(a -> a.id().equals(conjunta))
                        .findFirst()
                        .orElseThrow();

        assertThat(forAna.ownerUserId()).isEqualTo(ana.userId());
        assertThat(forAna.canUnshare()).isTrue();
        assertThat(forBia.ownerName()).isNotBlank();
        assertThat(forBia.canUnshare()).isFalse();
    }

    private HttpStatus deleteAs(TestUser user, Long householdId, String url) {
        return HttpStatus.valueOf(
                restTemplate
                        .exchange(
                                url,
                                HttpMethod.DELETE,
                                new HttpEntity<>(headers(user, householdId)),
                                String.class)
                        .getStatusCode()
                        .value());
    }

    private Long createTransactionIn(TestUser user, Long householdId, Long accountId) {
        return post(
                        user,
                        householdId,
                        "/api/transactions",
                        new TransactionRequest(
                                accountId,
                                null,
                                null,
                                "Lançamento",
                                BigDecimal.TEN,
                                LocalDate.now(),
                                CategoryType.EXPENSE),
                        TransactionResponse.class)
                .getBody()
                .id();
    }

    @Test
    void onlyTheAuthorCanDeleteATransactionOfASharedAccount() {
        TestUser ana = TestDataFactory.registerRandomUser(restTemplate);
        TestUser bia = TestDataFactory.registerRandomUser(restTemplate);
        Long sharedId = createShared(ana);
        join(ana, sharedId, bia);
        Long conjunta = createAccount(ana, "Conjunta");
        share(ana, sharedId, conjunta);
        Long byAna = createTransactionIn(ana, sharedId, conjunta);
        Long byBia = createTransactionIn(bia, sharedId, conjunta);

        assertThat(deleteAs(bia, sharedId, "/api/transactions/" + byAna))
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(deleteAs(ana, sharedId, "/api/transactions/" + byBia))
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(deleteAs(ana, sharedId, "/api/transactions/" + byAna))
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(deleteAs(bia, sharedId, "/api/transactions/" + byBia))
                .isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void theListShowsWhoCreatedEachTransaction() {
        TestUser ana = TestDataFactory.registerRandomUser(restTemplate);
        TestUser bia = TestDataFactory.registerRandomUser(restTemplate);
        Long sharedId = createShared(ana);
        join(ana, sharedId, bia);
        Long conjunta = createAccount(ana, "Conjunta");
        share(ana, sharedId, conjunta);
        Long byBia = createTransactionIn(bia, sharedId, conjunta);

        TransactionResponse seen =
                transactions(ana, sharedId).stream()
                        .filter(t -> t.id().equals(byBia))
                        .findFirst()
                        .orElseThrow();

        assertThat(seen.createdByUserId()).isEqualTo(bia.userId());
        assertThat(seen.createdByName()).isNotBlank();
    }

    @Test
    void transactionsCreatedBeforeSharingKeepTheirAuthor() {
        TestUser ana = TestDataFactory.registerRandomUser(restTemplate);
        TestUser bia = TestDataFactory.registerRandomUser(restTemplate);
        Long sharedId = createShared(ana);
        join(ana, sharedId, bia);
        Long conjunta = createAccount(ana, "Conjunta");
        Long oldTransaction = createTransactionIn(ana, null, conjunta);

        share(ana, sharedId, conjunta);

        assertThat(deleteAs(bia, sharedId, "/api/transactions/" + oldTransaction))
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(deleteAs(ana, sharedId, "/api/transactions/" + oldTransaction))
                .isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void onlyTheAuthorCanDeleteATransferBetweenSharedAccounts() {
        TestUser ana = TestDataFactory.registerRandomUser(restTemplate);
        TestUser bia = TestDataFactory.registerRandomUser(restTemplate);
        Long sharedId = createShared(ana);
        join(ana, sharedId, bia);
        Long corrente = createAccount(ana, "Corrente");
        Long poupanca = createAccount(ana, "Poupança");
        share(ana, sharedId, corrente, poupanca);
        Long transferId =
                post(
                                ana,
                                sharedId,
                                "/api/transfers",
                                new TransferRequest(
                                        corrente,
                                        poupanca,
                                        BigDecimal.valueOf(100),
                                        LocalDate.now(),
                                        null),
                                TransferResponse.class)
                        .getBody()
                        .id();

        assertThat(deleteAs(bia, sharedId, "/api/transfers/" + transferId))
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(deleteAs(ana, sharedId, "/api/transfers/" + transferId))
                .isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void aSavingsGoalLinkingAnAccountLeftBehindStillBlocksTheSharing() {
        TestUser ana = TestDataFactory.registerRandomUser(restTemplate);
        Long sharedId = createShared(ana);
        Long reserva = createAccount(ana, "Reserva", AccountType.RESERVE);
        Long corrente = createAccount(ana, "Corrente");
        ResponseEntity<String> goal =
                post(
                        ana,
                        null,
                        "/api/savings-goals",
                        java.util.Map.of(
                                "name",
                                "Viagem",
                                "type",
                                "VACATION",
                                "targetAmount",
                                5000,
                                "accountId",
                                reserva,
                                "fundingAccountId",
                                corrente),
                        String.class);
        assertThat(goal.getStatusCode().is2xxSuccessful()).as(goal.getBody()).isTrue();

        ResponseEntity<String> blocked =
                post(
                        ana,
                        null,
                        "/api/households/" + sharedId + "/accounts/share",
                        new ShareAccountsRequest(Set.of(reserva)),
                        String.class);

        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(blocked.getBody()).contains("Reserva").contains("Corrente");
        assertThat(accountIds(ana, null)).contains(reserva, corrente);
        assertThat(share(ana, sharedId, reserva, corrente).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void anyMemberCanShareTheirOwnAccounts() {
        TestUser ana = TestDataFactory.registerRandomUser(restTemplate);
        TestUser bia = TestDataFactory.registerRandomUser(restTemplate);
        Long sharedId = createShared(ana);
        join(ana, sharedId, bia);
        Long biaAccount = createAccount(bia, "Conta da Bia");

        assertThat(share(bia, sharedId, biaAccount).getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(accountIds(ana, sharedId)).contains(biaAccount);
    }

    @Test
    void nonMemberCannotShareIntoTheGroup() {
        TestUser ana = TestDataFactory.registerRandomUser(restTemplate);
        TestUser stranger = TestDataFactory.registerRandomUser(restTemplate);
        Long sharedId = createShared(ana);
        Long strangerAccount = createAccount(stranger, "Conta");

        assertThat(share(stranger, sharedId, strangerAccount).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(accountIds(stranger, null)).contains(strangerAccount);
    }

    @Test
    void cannotShareAnotherPersonsAccount() {
        TestUser ana = TestDataFactory.registerRandomUser(restTemplate);
        TestUser bia = TestDataFactory.registerRandomUser(restTemplate);
        Long sharedId = createShared(ana);
        join(ana, sharedId, bia);
        Long biaAccount = createAccount(bia, "Conta da Bia");

        assertThat(share(ana, sharedId, biaAccount).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(accountIds(bia, null)).contains(biaAccount);
    }

    @Test
    void cannotShareIntoThePersonalSpace() {
        TestUser ana = TestDataFactory.registerRandomUser(restTemplate);
        Long personalId =
                List.of(get(ana, null, "/api/households", HouseholdResponse[].class).getBody())
                        .get(0)
                        .id();
        Long accountId = createAccount(ana, "Conta");

        assertThat(share(ana, personalId, accountId).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void aMemberTransfersBetweenTheirPersonalAccountAndASharedAccountInBothDirections() {
        TestUser ana = TestDataFactory.registerRandomUser(restTemplate);
        TestUser bia = TestDataFactory.registerRandomUser(restTemplate);
        TestUser stranger = TestDataFactory.registerRandomUser(restTemplate);
        Long sharedId = createShared(ana);
        join(ana, sharedId, bia);
        Long conjunta = createAccount(ana, "Conjunta");
        assertThat(share(ana, sharedId, conjunta).getStatusCode()).isEqualTo(HttpStatus.OK);
        Long pessoalDaBia = createAccount(bia, "Pessoal da Bia");

        // Quem não é do grupo não enxerga a conta conjunta nem para transferir.
        assertThat(
                        post(
                                        stranger,
                                        null,
                                        "/api/transfers",
                                        new TransferRequest(
                                                createAccount(stranger, "Dele"),
                                                conjunta,
                                                BigDecimal.TEN,
                                                LocalDate.now(),
                                                null),
                                        String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        // A conta do grupo aparece como destino possível no espaço pessoal, e vice-versa.
        assertThat(
                        List.of(
                                        get(
                                                        bia,
                                                        null,
                                                        "/api/transfers/linkable-accounts",
                                                        LinkableAccountResponse[].class)
                                                .getBody())
                                .stream()
                                .map(LinkableAccountResponse::id))
                .containsExactly(conjunta);
        assertThat(
                        List.of(
                                        get(
                                                        bia,
                                                        sharedId,
                                                        "/api/transfers/linkable-accounts",
                                                        LinkableAccountResponse[].class)
                                                .getBody())
                                .stream()
                                .map(LinkableAccountResponse::id))
                .contains(pessoalDaBia);

        // Do espaço pessoal para a conta conjunta.
        assertThat(
                        post(
                                        bia,
                                        null,
                                        "/api/transfers",
                                        new TransferRequest(
                                                pessoalDaBia,
                                                conjunta,
                                                BigDecimal.valueOf(100),
                                                LocalDate.now(),
                                                null),
                                        String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(currentBalance(bia, null)).isEqualTo(900.0);
        assertThat(currentBalance(bia, sharedId)).isEqualTo(1100.0);
        // Dinheiro que muda de espaço é receita no grupo e despesa no espaço pessoal.
        assertThat(monthFlow(bia, sharedId, "currentMonthIncome")).isEqualTo(100.0);
        assertThat(monthFlow(bia, null, "currentMonthExpense")).isEqualTo(100.0);
        assertThat(transactions(bia, sharedId))
                .filteredOn(t -> t.accountId().equals(conjunta))
                .hasSize(1)
                .allSatisfy(t -> assertThat(t.type()).isEqualTo(CategoryType.INCOME));
        TransferResponse[] inGroup =
                get(bia, sharedId, "/api/transfers", TransferResponse[].class).getBody();
        assertThat(inGroup).hasSize(1);
        assertThat(inGroup[0].fromAccountName()).isEqualTo("Pessoal da Bia");
        assertThat(inGroup[0].toAccountName()).isEqualTo("Conjunta");

        // Da conta conjunta (no espaço do grupo) para a conta pessoal.
        assertThat(
                        post(
                                        bia,
                                        sharedId,
                                        "/api/transfers",
                                        new TransferRequest(
                                                conjunta,
                                                pessoalDaBia,
                                                BigDecimal.valueOf(50),
                                                LocalDate.now(),
                                                null),
                                        String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(currentBalance(bia, null)).isEqualTo(950.0);
        assertThat(currentBalance(bia, sharedId)).isEqualTo(1050.0);
        assertThat(monthFlow(bia, sharedId, "currentMonthExpense")).isEqualTo(50.0);
        assertThat(monthFlow(bia, null, "currentMonthIncome")).isEqualTo(50.0);
        // Cada espaço guarda só a sua metade: o pessoal enxerga as duas transferências.
        assertThat(get(bia, null, "/api/transfers", TransferResponse[].class).getBody()).hasSize(2);
        assertThat(get(bia, sharedId, "/api/transfers", TransferResponse[].class).getBody())
                .hasSize(2);
    }
}
