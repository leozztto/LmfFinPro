package com.lmf.finpro.integration.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.infrastructure.web.dto.auth.AuthResponse;
import com.lmf.finpro.infrastructure.web.dto.auth.LoginRequest;
import com.lmf.finpro.infrastructure.web.dto.profile.ChangePasswordRequest;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

// Tolerância zero: qualquer reapresentação de um token já rotacionado conta como reuso.
@TestPropertySource(properties = "finpro.refresh-token.reuse-leeway-seconds=0")
class RefreshTokenIntegrationTest extends AbstractIntegrationTest {

    private static final String COOKIE = "finpro_refresh";
    private static final String PASSWORD = "senha12345";

    /** Valor "nome=valor" do Set-Cookie do refresh token, pronto para o header Cookie. */
    private static String refreshCookie(ResponseEntity<?> response) {
        List<String> setCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(setCookies).isNotNull();
        return setCookies.stream()
                .filter(c -> c.startsWith(COOKIE + "="))
                .map(c -> c.substring(0, c.indexOf(';')))
                .findFirst()
                .orElseThrow();
    }

    private ResponseEntity<AuthResponse> post(String path, String cookie) {
        HttpHeaders headers = new HttpHeaders();
        if (cookie != null) {
            headers.add(HttpHeaders.COOKIE, cookie);
        }
        return restTemplate.exchange(
                path, HttpMethod.POST, new HttpEntity<>(headers), AuthResponse.class);
    }

    private ResponseEntity<AuthResponse> login(TestUser user) {
        return restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(user.email(), PASSWORD), AuthResponse.class);
    }

    @Test
    void loginSetsAnHttpOnlyScopedRefreshCookie() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        ResponseEntity<AuthResponse> response = login(user);

        String setCookie =
                response.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
                        .filter(c -> c.startsWith(COOKIE + "="))
                        .findFirst()
                        .orElseThrow();
        assertThat(setCookie)
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Strict")
                .contains("Path=/api/auth")
                .contains("Max-Age=");
        // O refresh token nunca vai no corpo — só o access token.
        assertThat(response.getBody().token()).isNotBlank();
    }

    @Test
    void refreshIssuesANewAccessTokenThatWorksAndRotatesTheCookie() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        String cookie = refreshCookie(login(user));

        ResponseEntity<AuthResponse> refreshed = post("/api/auth/refresh", cookie);

        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshed.getBody().email()).isEqualTo(user.email());
        assertThat(refreshCookie(refreshed)).isNotEqualTo(cookie);

        HttpHeaders bearer = new HttpHeaders();
        bearer.setBearerAuth(refreshed.getBody().token());
        ResponseEntity<String> profile =
                restTemplate.exchange(
                        "/api/profile", HttpMethod.GET, new HttpEntity<>(bearer), String.class);
        assertThat(profile.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void reusingARotatedCookieIsRejectedAndKillsTheWholeSession() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        String original = refreshCookie(login(user));
        String rotated = refreshCookie(post("/api/auth/refresh", original));

        assertThat(post("/api/auth/refresh", original).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        // O token legítimo mais novo também caiu: não dá para saber quem é o dono de verdade.
        assertThat(post("/api/auth/refresh", rotated).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refreshWithoutOrWithGarbageCookieReturns401AndClearsTheCookie() {
        ResponseEntity<AuthResponse> none = post("/api/auth/refresh", null);
        ResponseEntity<AuthResponse> garbage = post("/api/auth/refresh", COOKIE + "=lixo");

        assertThat(none.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(garbage.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(garbage.getHeaders().get(HttpHeaders.SET_COOKIE).get(0))
                .startsWith(COOKIE + "=;")
                .contains("Max-Age=0");
    }

    @Test
    void logoutRevokesTheRefreshToken() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        String cookie = refreshCookie(login(user));

        ResponseEntity<AuthResponse> logout = post("/api/auth/logout", cookie);

        assertThat(logout.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(post("/api/auth/refresh", cookie).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void changingThePasswordInvalidatesOtherSessionsButKeepsTheCurrentOne() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);
        String otherDevice = refreshCookie(login(user));

        HttpEntity<ChangePasswordRequest> request =
                new HttpEntity<>(
                        new ChangePasswordRequest(PASSWORD, "outraSenha123"), user.authHeaders());
        ResponseEntity<AuthResponse> changed =
                restTemplate.exchange(
                        "/api/profile/password", HttpMethod.PUT, request, AuthResponse.class);

        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(post("/api/auth/refresh", otherDevice).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(post("/api/auth/refresh", refreshCookie(changed)).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }
}
