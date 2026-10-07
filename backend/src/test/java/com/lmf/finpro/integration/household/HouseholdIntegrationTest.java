package com.lmf.finpro.integration.household;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.BrazilianState;
import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.model.HouseholdRole;
import com.lmf.finpro.domain.model.HouseholdType;
import com.lmf.finpro.domain.model.TaxRegime;
import com.lmf.finpro.domain.port.out.HouseholdInviteMailerPort;
import com.lmf.finpro.infrastructure.security.JwtAuthenticationFilter;
import com.lmf.finpro.infrastructure.web.dto.account.AccountRequest;
import com.lmf.finpro.infrastructure.web.dto.account.AccountResponse;
import com.lmf.finpro.infrastructure.web.dto.auth.AddressRequest;
import com.lmf.finpro.infrastructure.web.dto.auth.AuthResponse;
import com.lmf.finpro.infrastructure.web.dto.auth.RegisterRequest;
import com.lmf.finpro.infrastructure.web.dto.household.AcceptInviteRequest;
import com.lmf.finpro.infrastructure.web.dto.household.HouseholdMemberResponse;
import com.lmf.finpro.infrastructure.web.dto.household.HouseholdRequest;
import com.lmf.finpro.infrastructure.web.dto.household.HouseholdResponse;
import com.lmf.finpro.infrastructure.web.dto.household.InviteRequest;
import com.lmf.finpro.infrastructure.web.dto.household.ReceivedInviteResponse;
import com.lmf.finpro.infrastructure.web.dto.household.TransferOwnershipRequest;
import com.lmf.finpro.infrastructure.web.exception.ApiError;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.CpfTestFactory;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Fluxo completo de grupos compartilhados: criar, convidar, entrar, alternar o grupo ativo pelo
 * header e gerir membros — sempre pela API, como o frontend.
 */
class HouseholdIntegrationTest extends AbstractIntegrationTest {

    /** Guarda o link de cada convite por e-mail (o token em claro só existe no e-mail). */
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

    private Long createShared(TestUser owner, String name) {
        return restTemplate
                .exchange(
                        "/api/households",
                        HttpMethod.POST,
                        new HttpEntity<>(new HouseholdRequest(name), owner.authHeaders()),
                        HouseholdResponse.class)
                .getBody()
                .id();
    }

    private ResponseEntity<String> invite(TestUser owner, Long householdId, String email) {
        return restTemplate.exchange(
                "/api/households/" + householdId + "/invites",
                HttpMethod.POST,
                new HttpEntity<>(new InviteRequest(email), owner.authHeaders()),
                String.class);
    }

    private ResponseEntity<HouseholdResponse> accept(TestUser user, String token) {
        return restTemplate.exchange(
                "/api/households/invites/accept",
                HttpMethod.POST,
                new HttpEntity<>(new AcceptInviteRequest(token), user.authHeaders()),
                HouseholdResponse.class);
    }

    /** Convida e aceita: a pessoa passa a ser membro do grupo. */
    private void join(TestUser owner, Long householdId, TestUser member) {
        assertThat(invite(owner, householdId, member.email()).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(accept(member, mailer.tokenByEmail.get(member.email())).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    private Long createAccount(TestUser user, Long householdId, String name) {
        return restTemplate
                .exchange(
                        "/api/accounts",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new AccountRequest(name, AccountType.CHECKING, BigDecimal.TEN),
                                headers(user, householdId)),
                        AccountResponse.class)
                .getBody()
                .id();
    }

    private HttpStatus listAccountsStatus(TestUser user, Long householdId) {
        return HttpStatus.valueOf(
                restTemplate
                        .exchange(
                                "/api/accounts",
                                HttpMethod.GET,
                                new HttpEntity<>(headers(user, householdId)),
                                String.class)
                        .getStatusCode()
                        .value());
    }

    private ResponseEntity<AccountResponse[]> listAccounts(TestUser user, Long householdId) {
        return restTemplate.exchange(
                "/api/accounts",
                HttpMethod.GET,
                new HttpEntity<>(headers(user, householdId)),
                AccountResponse[].class);
    }

    private List<Long> accountIds(TestUser user, Long householdId) {
        return List.of(listAccounts(user, householdId).getBody()).stream()
                .map(AccountResponse::id)
                .toList();
    }

    @Test
    void newUserHasOnlyThePersonalHouseholdAndCanCreateASharedOne() {
        TestUser ana = TestDataFactory.registerRandomUser(restTemplate);

        Long sharedId = createShared(ana, "Família Silva");

        HouseholdResponse[] mine =
                restTemplate
                        .exchange(
                                "/api/households",
                                HttpMethod.GET,
                                new HttpEntity<>(ana.authHeaders()),
                                HouseholdResponse[].class)
                        .getBody();
        assertThat(List.of(mine))
                .extracting(HouseholdResponse::type, HouseholdResponse::role)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(
                                HouseholdType.PERSONAL, HouseholdRole.OWNER),
                        org.assertj.core.groups.Tuple.tuple(
                                HouseholdType.SHARED, HouseholdRole.OWNER));
        assertThat(List.of(mine)).extracting(HouseholdResponse::id).contains(sharedId);
    }

    @Test
    void membersSeeTheSharedDataWhileTheStrangerAndTheirOwnPersonalSpaceDoNot() {
        TestUser ana = TestDataFactory.registerRandomUser(restTemplate);
        TestUser bia = TestDataFactory.registerRandomUser(restTemplate);
        TestUser stranger = TestDataFactory.registerRandomUser(restTemplate);
        Long sharedId = createShared(ana, "Família Silva");
        join(ana, sharedId, bia);

        Long sharedAccount = createAccount(ana, sharedId, "Conta da casa");
        Long anaPersonalAccount = createAccount(ana, null, "Só da Ana");
        Long biaPersonalAccount = createAccount(bia, null, "Só da Bia");

        // Bia vê a conta da casa ao escolher o grupo, mas não a pessoal da Ana.
        assertThat(accountIds(bia, sharedId))
                .contains(sharedAccount)
                .doesNotContain(anaPersonalAccount, biaPersonalAccount);
        // Sem o header, cada um vê apenas o seu espaço pessoal.
        assertThat(accountIds(bia, null))
                .contains(biaPersonalAccount)
                .doesNotContain(sharedAccount, anaPersonalAccount);
        assertThat(accountIds(ana, null))
                .contains(anaPersonalAccount)
                .doesNotContain(sharedAccount, biaPersonalAccount);
        // Quem não é membro não entra no grupo nem por header.
        assertThat(listAccountsStatus(stranger, sharedId)).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void invalidHouseholdHeaderIsForbidden() {
        TestUser ana = TestDataFactory.registerRandomUser(restTemplate);

        HttpHeaders bad = ana.authHeaders();
        bad.add(JwtAuthenticationFilter.HOUSEHOLD_HEADER, "abc");

        assertThat(
                        restTemplate
                                .exchange(
                                        "/api/accounts",
                                        HttpMethod.GET,
                                        new HttpEntity<>(bad),
                                        String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void personalHouseholdDoesNotAcceptInvites() {
        TestUser ana = TestDataFactory.registerRandomUser(restTemplate);
        Long personalId =
                List.of(
                                restTemplate
                                        .exchange(
                                                "/api/households",
                                                HttpMethod.GET,
                                                new HttpEntity<>(ana.authHeaders()),
                                                HouseholdResponse[].class)
                                        .getBody())
                        .get(0)
                        .id();

        assertThat(invite(ana, personalId, "x@finpro.test").getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void onlyTheOwnerCanInvite() {
        TestUser ana = TestDataFactory.registerRandomUser(restTemplate);
        TestUser bia = TestDataFactory.registerRandomUser(restTemplate);
        Long sharedId = createShared(ana, "Família Silva");
        join(ana, sharedId, bia);

        assertThat(invite(bia, sharedId, "outro@finpro.test").getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void inviteIsSingleUse() {
        TestUser ana = TestDataFactory.registerRandomUser(restTemplate);
        TestUser bia = TestDataFactory.registerRandomUser(restTemplate);
        TestUser carla = TestDataFactory.registerRandomUser(restTemplate);
        Long sharedId = createShared(ana, "Família Silva");
        join(ana, sharedId, bia);
        String usedToken = mailer.tokenByEmail.get(bia.email());

        assertThat(accept(carla, usedToken).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void unknownTokenIsRejected() {
        TestUser bia = TestDataFactory.registerRandomUser(restTemplate);

        assertThat(accept(bia, "token-que-nao-existe").getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void registeringWithAnInviteTokenJoinsTheSharedGroup() {
        TestUser ana = TestDataFactory.registerRandomUser(restTemplate);
        Long sharedId = createShared(ana, "Família Silva");
        String newcomerEmail = "novo-" + UUID.randomUUID() + "@finpro.test";
        assertThat(invite(ana, sharedId, newcomerEmail).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        ResponseEntity<AuthResponse> registered =
                register(newcomerEmail, mailer.tokenByEmail.get(newcomerEmail));

        assertThat(registered.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        TestUser newcomer =
                new TestUser(
                        registered.getBody().userId(), newcomerEmail, registered.getBody().token());
        Long sharedAccount = createAccount(ana, sharedId, "Conta da casa");
        assertThat(accountIds(newcomer, sharedId)).contains(sharedAccount);
    }

    @Test
    void registeringWithAnInvalidInviteTokenFailsAndCreatesNoUser() {
        String email = "falha-" + UUID.randomUUID() + "@finpro.test";

        ResponseEntity<ApiError> response =
                restTemplate.postForEntity(
                        "/api/auth/register",
                        registerRequest(email, "convite-invalido"),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(
                        restTemplate
                                .postForEntity(
                                        "/api/auth/login",
                                        Map.of("email", email, "password", "senha12345"),
                                        String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void removedMemberLosesAccessToTheGroupData() {
        TestUser ana = TestDataFactory.registerRandomUser(restTemplate);
        TestUser bia = TestDataFactory.registerRandomUser(restTemplate);
        Long sharedId = createShared(ana, "Família Silva");
        join(ana, sharedId, bia);
        assertThat(listAccountsStatus(bia, sharedId)).isEqualTo(HttpStatus.OK);

        ResponseEntity<Void> removed =
                restTemplate.exchange(
                        "/api/households/" + sharedId + "/members/" + bia.userId(),
                        HttpMethod.DELETE,
                        new HttpEntity<>(ana.authHeaders()),
                        Void.class);

        assertThat(removed.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(listAccountsStatus(bia, sharedId)).isEqualTo(HttpStatus.FORBIDDEN);
        // O espaço pessoal da Bia continua intacto.
        assertThat(listAccountsStatus(bia, null)).isEqualTo(HttpStatus.OK);
    }

    @Test
    void ownerMustTransferOwnershipBeforeLeaving() {
        TestUser ana = TestDataFactory.registerRandomUser(restTemplate);
        TestUser bia = TestDataFactory.registerRandomUser(restTemplate);
        Long sharedId = createShared(ana, "Família Silva");
        join(ana, sharedId, bia);

        assertThat(leave(ana, sharedId).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<Void> transferred =
                restTemplate.exchange(
                        "/api/households/" + sharedId + "/owner",
                        HttpMethod.PUT,
                        new HttpEntity<>(
                                new TransferOwnershipRequest(bia.userId()), ana.authHeaders()),
                        Void.class);
        assertThat(transferred.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(leave(ana, sharedId).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        HouseholdMemberResponse[] members =
                restTemplate
                        .exchange(
                                "/api/households/" + sharedId + "/members",
                                HttpMethod.GET,
                                new HttpEntity<>(bia.authHeaders()),
                                HouseholdMemberResponse[].class)
                        .getBody();
        assertThat(List.of(members))
                .singleElement()
                .satisfies(
                        member -> {
                            assertThat(member.userId()).isEqualTo(bia.userId());
                            assertThat(member.role()).isEqualTo(HouseholdRole.OWNER);
                        });
    }

    @Test
    void nonMemberCannotListMembers() {
        TestUser ana = TestDataFactory.registerRandomUser(restTemplate);
        TestUser stranger = TestDataFactory.registerRandomUser(restTemplate);
        Long sharedId = createShared(ana, "Família Silva");

        assertThat(
                        restTemplate
                                .exchange(
                                        "/api/households/" + sharedId + "/members",
                                        HttpMethod.GET,
                                        new HttpEntity<>(stranger.authHeaders()),
                                        String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private List<ReceivedInviteResponse> receivedInvites(TestUser user) {
        return List.of(
                restTemplate
                        .exchange(
                                "/api/households/invites/received",
                                HttpMethod.GET,
                                new HttpEntity<>(user.authHeaders()),
                                ReceivedInviteResponse[].class)
                        .getBody());
    }

    private ResponseEntity<String> postInviteAction(TestUser user, Long inviteId, String action) {
        return restTemplate.exchange(
                "/api/households/invites/" + inviteId + "/" + action,
                HttpMethod.POST,
                new HttpEntity<>(user.authHeaders()),
                String.class);
    }

    @Test
    void anInvitedUserSeesAndAcceptsTheInviteInsideTheApp() {
        TestUser ana = TestDataFactory.registerRandomUser(restTemplate);
        TestUser bia = TestDataFactory.registerRandomUser(restTemplate);
        Long sharedId = createShared(ana, "Família Silva");
        assertThat(receivedInvites(bia)).isEmpty();
        invite(ana, sharedId, bia.email());

        List<ReceivedInviteResponse> received = receivedInvites(bia);

        assertThat(received)
                .singleElement()
                .satisfies(
                        invite -> {
                            assertThat(invite.householdId()).isEqualTo(sharedId);
                            assertThat(invite.householdName()).isEqualTo("Família Silva");
                            assertThat(invite.inviterName()).isNotBlank();
                        });
        // O que o app mostra não traz o token nem o hash: o link continua sendo o segredo.
        assertThat(postInviteAction(bia, received.get(0).id(), "accept").getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(receivedInvites(bia)).isEmpty();
        assertThat(
                        restTemplate
                                .exchange(
                                        "/api/households/" + sharedId + "/members",
                                        HttpMethod.GET,
                                        new HttpEntity<>(bia.authHeaders()),
                                        HouseholdMemberResponse[].class)
                                .getBody())
                .hasSize(2);
    }

    @Test
    void decliningAReceivedInviteRemovesItAndKillsTheLink() {
        TestUser ana = TestDataFactory.registerRandomUser(restTemplate);
        TestUser bia = TestDataFactory.registerRandomUser(restTemplate);
        Long sharedId = createShared(ana, "Família Silva");
        invite(ana, sharedId, bia.email());
        String linkToken = mailer.tokenByEmail.get(bia.email());
        Long inviteId = receivedInvites(bia).get(0).id();

        assertThat(postInviteAction(bia, inviteId, "decline").getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(receivedInvites(bia)).isEmpty();
        assertThat(accept(bia, linkToken).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void anInviteForAnotherEmailCannotBeAcceptedOrDeclinedById() {
        TestUser ana = TestDataFactory.registerRandomUser(restTemplate);
        TestUser bia = TestDataFactory.registerRandomUser(restTemplate);
        TestUser stranger = TestDataFactory.registerRandomUser(restTemplate);
        Long sharedId = createShared(ana, "Família Silva");
        invite(ana, sharedId, bia.email());
        Long inviteId = receivedInvites(bia).get(0).id();

        assertThat(receivedInvites(stranger)).isEmpty();
        assertThat(postInviteAction(stranger, inviteId, "accept").getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(postInviteAction(stranger, inviteId, "decline").getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        // O convite continua lá para quem de direito.
        assertThat(receivedInvites(bia)).hasSize(1);
    }

    private ResponseEntity<Void> leave(TestUser user, Long householdId) {
        return restTemplate.exchange(
                "/api/households/" + householdId + "/leave",
                HttpMethod.POST,
                new HttpEntity<>(user.authHeaders()),
                Void.class);
    }

    private ResponseEntity<AuthResponse> register(String email, String inviteToken) {
        return restTemplate.postForEntity(
                "/api/auth/register", registerRequest(email, inviteToken), AuthResponse.class);
    }

    private RegisterRequest registerRequest(String email, String inviteToken) {
        return new RegisterRequest(
                "Usuário Convidado",
                email,
                "senha12345",
                DocumentType.CPF,
                CpfTestFactory.randomValidCpf(),
                "11987654321",
                TaxRegime.AUTONOMO,
                new AddressRequest(
                        "01310100",
                        "Avenida Paulista",
                        "1000",
                        "Sala 1",
                        "Bela Vista",
                        "São Paulo",
                        BrazilianState.SP),
                inviteToken);
    }
}
