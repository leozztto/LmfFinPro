package com.lmf.finpro.integration.profile;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.model.TaxRegime;
import com.lmf.finpro.infrastructure.web.dto.auth.AuthResponse;
import com.lmf.finpro.infrastructure.web.dto.auth.LoginRequest;
import com.lmf.finpro.infrastructure.web.dto.profile.ChangePasswordRequest;
import com.lmf.finpro.infrastructure.web.dto.profile.ProfileResponse;
import com.lmf.finpro.infrastructure.web.dto.profile.UpdateProfileRequest;
import com.lmf.finpro.infrastructure.web.exception.ApiError;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.CnpjTestFactory;
import com.lmf.finpro.integration.support.CpfTestFactory;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ProfileIntegrationTest extends AbstractIntegrationTest {

    private ResponseEntity<ProfileResponse> getProfile(HttpHeaders headers) {
        return restTemplate.exchange(
                "/api/profile", HttpMethod.GET, new HttpEntity<>(headers), ProfileResponse.class);
    }

    private <T> ResponseEntity<T> put(
            String path, Object body, HttpHeaders headers, Class<T> type) {
        return restTemplate.exchange(path, HttpMethod.PUT, new HttpEntity<>(body, headers), type);
    }

    private static UpdateProfileRequest request(
            String email,
            DocumentType documentType,
            String document,
            TaxRegime regime,
            String currentPassword) {
        return new UpdateProfileRequest(
                "Nome Atualizado",
                email,
                documentType,
                document,
                "11912345678",
                regime,
                TestDataFactory.sampleAddress(),
                currentPassword);
    }

    @Test
    void getProfileReturnsTheLoggedUserData() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        ResponseEntity<ProfileResponse> response = getProfile(user.authHeaders());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().email()).isEqualTo(user.email());
        assertThat(response.getBody().address()).isNotNull();
    }

    @Test
    void getProfileWithoutTokenReturnsUnauthorized() {
        ResponseEntity<ApiError> response =
                restTemplate.getForEntity("/api/profile", ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void updateProfileChangesDataIncludingRegimeAndDocument() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        String cnpj = CnpjTestFactory.randomValidCnpj();

        ResponseEntity<ProfileResponse> response =
                put(
                        "/api/profile",
                        request(user.email(), DocumentType.CNPJ, cnpj, TaxRegime.MEI, null),
                        user.authHeaders(),
                        ProfileResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().name()).isEqualTo("Nome Atualizado");
        assertThat(response.getBody().taxRegime()).isEqualTo(TaxRegime.MEI);
        assertThat(response.getBody().documentNumber()).isEqualTo(cnpj);
        assertThat(getProfile(user.authHeaders()).getBody().name()).isEqualTo("Nome Atualizado");
    }

    @Test
    void updateProfileWithDocumentIncompatibleWithRegimeReturnsBadRequest() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        ResponseEntity<ApiError> response =
                put(
                        "/api/profile",
                        request(
                                user.email(),
                                DocumentType.CPF,
                                CpfTestFactory.randomValidCpf(),
                                TaxRegime.MEI,
                                null),
                        user.authHeaders(),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void changingEmailWithoutCurrentPasswordReturnsBadRequestAndKeepsSession() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        ResponseEntity<ApiError> response =
                put(
                        "/api/profile",
                        request(
                                "novo-" + UUID.randomUUID() + "@finpro.test",
                                DocumentType.CPF,
                                CpfTestFactory.randomValidCpf(),
                                TaxRegime.AUTONOMO,
                                null),
                        user.authHeaders(),
                        ApiError.class);

        // 400, não 401 — um 401 faria o frontend derrubar a sessão.
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(getProfile(user.authHeaders()).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void changingEmailWithCurrentPasswordAllowsLoginWithTheNewEmail() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        String newEmail = "novo-" + UUID.randomUUID() + "@finpro.test";

        ResponseEntity<ProfileResponse> response =
                put(
                        "/api/profile",
                        request(
                                newEmail,
                                DocumentType.CPF,
                                CpfTestFactory.randomValidCpf(),
                                TaxRegime.AUTONOMO,
                                "senha12345"),
                        user.authHeaders(),
                        ProfileResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<AuthResponse> login =
                restTemplate.postForEntity(
                        "/api/auth/login",
                        new LoginRequest(newEmail, "senha12345"),
                        AuthResponse.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void changePasswordKeepsCurrentSessionWithNewTokenAndEndsTheOthers() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        AuthResponse otherDevice =
                restTemplate.postForObject(
                        "/api/auth/login",
                        new LoginRequest(user.email(), "senha12345"),
                        AuthResponse.class);

        ResponseEntity<AuthResponse> response =
                put(
                        "/api/profile/password",
                        new ChangePasswordRequest("senha12345", "novaSenha123"),
                        user.authHeaders(),
                        AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        TestUser currentSession =
                new TestUser(user.userId(), user.email(), response.getBody().token());
        assertThat(getProfile(currentSession.authHeaders()).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        TestUser otherSession = new TestUser(user.userId(), user.email(), otherDevice.token());
        assertThat(getProfile(otherSession.authHeaders()).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void changePasswordWithWrongCurrentPasswordReturnsBadRequest() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        ResponseEntity<ApiError> response =
                put(
                        "/api/profile/password",
                        new ChangePasswordRequest("errada123", "novaSenha123"),
                        user.authHeaders(),
                        ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("Senha atual incorreta");
    }
}
