package com.lmf.finpro.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.exception.InvalidTokenException;
import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.model.RefreshToken;
import com.lmf.finpro.domain.model.TaxRegime;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.RefreshTokenRepositoryPort;
import com.lmf.finpro.domain.port.out.TokenPort;
import com.lmf.finpro.domain.port.out.UserRepositoryPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenApplicationServiceTest {

    private static final Instant NOW_INSTANT = Instant.parse("2026-01-10T12:00:00Z");
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(NOW_INSTANT, ZoneOffset.UTC);
    private static final String RAW = "raw-refresh-token";

    @Mock private RefreshTokenRepositoryPort refreshTokenRepositoryPort;
    @Mock private UserRepositoryPort userRepositoryPort;
    @Mock private TokenPort tokenPort;

    private RefreshTokenApplicationService service;

    @BeforeEach
    void setUp() {
        service =
                new RefreshTokenApplicationService(
                        refreshTokenRepositoryPort,
                        userRepositoryPort,
                        tokenPort,
                        new RefreshTokenSettings(30, 10),
                        Clock.fixed(NOW_INSTANT, ZoneOffset.UTC));
    }

    private static User user(int sessionVersion) {
        return new User(
                1L,
                "Ana",
                "ana@finpro.test",
                "hash",
                DocumentType.CPF,
                "52998224725",
                null,
                TaxRegime.AUTONOMO,
                null,
                NOW,
                sessionVersion);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static RefreshToken stored(
            int sessionVersion, LocalDateTime expiresAt, LocalDateTime revokedAt) {
        return new RefreshToken(
                10L, 1L, sha256(RAW), "family-1", sessionVersion, expiresAt, revokedAt, NOW);
    }

    private void givenToken(RefreshToken token) {
        when(refreshTokenRepositoryPort.findByTokenHash(sha256(RAW)))
                .thenReturn(Optional.of(token));
    }

    @Test
    void startSessionStoresOnlyTheHashOfANewOpaqueToken() {
        when(refreshTokenRepositoryPort.save(any())).thenAnswer(i -> i.getArgument(0));

        String raw = service.startSession(user(2));

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepositoryPort).save(saved.capture());
        assertThat(raw).isNotBlank();
        assertThat(saved.getValue().tokenHash()).isEqualTo(sha256(raw)).isNotEqualTo(raw);
        assertThat(saved.getValue().sessionVersion()).isEqualTo(2);
        assertThat(saved.getValue().expiresAt()).isEqualTo(NOW.plusDays(30));
    }

    @Test
    void refreshRotatesTokenKeepingTheFamily() {
        givenToken(stored(0, NOW.plusDays(1), null));
        when(userRepositoryPort.findById(1L)).thenReturn(Optional.of(user(0)));
        when(tokenPort.generate(1L, "ana@finpro.test", 0)).thenReturn("new-access");
        when(refreshTokenRepositoryPort.save(any())).thenAnswer(i -> i.getArgument(0));

        AuthResult result = service.refresh(RAW);

        assertThat(result.token()).isEqualTo("new-access");
        assertThat(result.refreshToken()).isNotBlank().isNotEqualTo(RAW);
        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepositoryPort, org.mockito.Mockito.times(2)).save(saved.capture());
        RefreshToken revoked = saved.getAllValues().get(0);
        RefreshToken next = saved.getAllValues().get(1);
        assertThat(revoked.revokedAt()).isEqualTo(NOW);
        assertThat(next.familyId()).isEqualTo("family-1");
        assertThat(next.revokedAt()).isNull();
    }

    @Test
    void refreshWithUnknownOrBlankTokenIsRejected() {
        when(refreshTokenRepositoryPort.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh(RAW)).isInstanceOf(InvalidTokenException.class);
        assertThatThrownBy(() -> service.refresh(null)).isInstanceOf(InvalidTokenException.class);
        assertThatThrownBy(() -> service.refresh(" ")).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void reusingARotatedTokenOutsideTheLeewayRevokesTheWholeFamily() {
        givenToken(stored(0, NOW.plusDays(1), NOW.minusSeconds(60)));

        assertThatThrownBy(() -> service.refresh(RAW)).isInstanceOf(InvalidTokenException.class);

        verify(refreshTokenRepositoryPort).revokeFamily("family-1", NOW);
        verify(tokenPort, never()).generate(any(), any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void reusingARotatedTokenWithinTheLeewayIssuesOnlyAnAccessToken() {
        givenToken(stored(0, NOW.plusDays(1), NOW.minusSeconds(3)));
        when(userRepositoryPort.findById(1L)).thenReturn(Optional.of(user(0)));
        when(tokenPort.generate(1L, "ana@finpro.test", 0)).thenReturn("new-access");

        AuthResult result = service.refresh(RAW);

        assertThat(result.token()).isEqualTo("new-access");
        assertThat(result.refreshToken()).isNull();
        verify(refreshTokenRepositoryPort, never()).save(any());
        verify(refreshTokenRepositoryPort, never()).revokeFamily(any(), any());
    }

    @Test
    void expiredTokenIsRejected() {
        givenToken(stored(0, NOW.minusSeconds(1), null));

        assertThatThrownBy(() -> service.refresh(RAW)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void tokenFromAnOlderSessionVersionIsRejectedAndRevoked() {
        givenToken(stored(0, NOW.plusDays(1), null));
        when(userRepositoryPort.findById(1L)).thenReturn(Optional.of(user(1)));

        assertThatThrownBy(() -> service.refresh(RAW)).isInstanceOf(InvalidTokenException.class);

        verify(refreshTokenRepositoryPort).revokeFamily("family-1", NOW);
    }

    @Test
    void tokenOfADeletedUserIsRejected() {
        givenToken(stored(0, NOW.plusDays(1), null));
        when(userRepositoryPort.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh(RAW)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void endSessionRevokesTheWholeFamilyAndIgnoresUnknownTokens() {
        givenToken(stored(0, NOW.plusDays(1), null));

        service.endSession(RAW);
        service.endSession(null);

        verify(refreshTokenRepositoryPort).revokeFamily(eq("family-1"), eq(NOW));
    }
}
