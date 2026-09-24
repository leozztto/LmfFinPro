package com.lmf.finpro.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.exception.InvalidPasswordResetTokenException;
import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.model.PasswordResetToken;
import com.lmf.finpro.domain.model.TaxRegime;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.PasswordHasherPort;
import com.lmf.finpro.domain.port.out.PasswordResetMailerPort;
import com.lmf.finpro.domain.port.out.PasswordResetTokenRepositoryPort;
import com.lmf.finpro.domain.port.out.UserRepositoryPort;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PasswordResetApplicationServiceTest {

    private static final String RESET_PAGE = "http://localhost/redefinir-senha";

    @Mock private UserRepositoryPort userRepositoryPort;
    @Mock private PasswordResetTokenRepositoryPort tokenRepositoryPort;
    @Mock private PasswordHasherPort passwordHasherPort;
    @Mock private PasswordResetMailerPort mailerPort;

    private PasswordResetApplicationService service;

    @BeforeEach
    void setUp() {
        service =
                new PasswordResetApplicationService(
                        userRepositoryPort,
                        tokenRepositoryPort,
                        passwordHasherPort,
                        mailerPort,
                        new PasswordResetSettings(RESET_PAGE, 30));
    }

    private static User sampleUser() {
        return new User(
                1L,
                "Ana Freelancer",
                "ana@finpro.test",
                "old-hash",
                DocumentType.CPF,
                "52998224725",
                null,
                TaxRegime.AUTONOMO,
                null,
                LocalDateTime.now(),
                0);
    }

    @Test
    void requestResetStoresOnlyTheTokenHashAndEmailsTheRawToken() {
        when(userRepositoryPort.findByEmail("ana@finpro.test"))
                .thenReturn(Optional.of(sampleUser()));

        service.requestReset("ana@finpro.test");

        verify(tokenRepositoryPort).invalidateActiveTokens(eq(1L), any());
        ArgumentCaptor<PasswordResetToken> saved =
                ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepositoryPort).save(saved.capture());
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(mailerPort)
                .sendResetLink(
                        eq("ana@finpro.test"), eq("Ana Freelancer"), link.capture(), eq(30L));

        String rawToken = link.getValue().substring((RESET_PAGE + "?token=").length());
        assertThat(link.getValue()).startsWith(RESET_PAGE + "?token=");
        assertThat(saved.getValue().tokenHash()).hasSize(64).isNotEqualTo(rawToken);
        assertThat(saved.getValue().expiresAt())
                .isEqualTo(saved.getValue().createdAt().plusMinutes(30));
    }

    @Test
    void requestResetForUnknownEmailDoesNothingAndDoesNotFail() {
        when(userRepositoryPort.findByEmail("nobody@finpro.test")).thenReturn(Optional.empty());

        service.requestReset("nobody@finpro.test");

        verifyNoInteractions(tokenRepositoryPort, mailerPort);
    }

    @Test
    void requestResetSwallowsMailFailures() {
        when(userRepositoryPort.findByEmail("ana@finpro.test"))
                .thenReturn(Optional.of(sampleUser()));
        doThrow(new RuntimeException("SMTP down"))
                .when(mailerPort)
                .sendResetLink(anyString(), anyString(), anyString(), anyLong());

        service.requestReset("ana@finpro.test");

        verify(tokenRepositoryPort).save(any());
    }

    @Test
    void resetPasswordUpdatesHashAndMarksTokenAsUsed() {
        PasswordResetToken token =
                new PasswordResetToken(
                        5L,
                        1L,
                        "hash",
                        LocalDateTime.now().plusMinutes(10),
                        null,
                        LocalDateTime.now());
        when(tokenRepositoryPort.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(userRepositoryPort.findById(1L)).thenReturn(Optional.of(sampleUser()));
        when(passwordHasherPort.hash("novaSenha123")).thenReturn("new-hash");

        service.resetPassword("raw-token", "novaSenha123");

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepositoryPort).save(savedUser.capture());
        assertThat(savedUser.getValue().passwordHash()).isEqualTo("new-hash");
        assertThat(savedUser.getValue().sessionVersion()).isEqualTo(1);
        ArgumentCaptor<PasswordResetToken> savedToken =
                ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepositoryPort).save(savedToken.capture());
        assertThat(savedToken.getValue().usedAt()).isNotNull();
    }

    @Test
    void resetPasswordRejectsExpiredToken() {
        PasswordResetToken expired =
                new PasswordResetToken(
                        5L,
                        1L,
                        "hash",
                        LocalDateTime.now().minusMinutes(1),
                        null,
                        LocalDateTime.now().minusMinutes(31));
        when(tokenRepositoryPort.findByTokenHash(anyString())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.resetPassword("raw-token", "novaSenha123"))
                .isInstanceOf(InvalidPasswordResetTokenException.class);
        verify(userRepositoryPort, never()).save(any());
    }

    @Test
    void resetPasswordRejectsAlreadyUsedToken() {
        PasswordResetToken used =
                new PasswordResetToken(
                        5L,
                        1L,
                        "hash",
                        LocalDateTime.now().plusMinutes(10),
                        LocalDateTime.now().minusMinutes(1),
                        LocalDateTime.now().minusMinutes(5));
        when(tokenRepositoryPort.findByTokenHash(anyString())).thenReturn(Optional.of(used));

        assertThatThrownBy(() -> service.resetPassword("raw-token", "novaSenha123"))
                .isInstanceOf(InvalidPasswordResetTokenException.class);
    }

    @Test
    void resetPasswordRejectsUnknownToken() {
        when(tokenRepositoryPort.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword("raw-token", "novaSenha123"))
                .isInstanceOf(InvalidPasswordResetTokenException.class);
    }
}
