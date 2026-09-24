package com.lmf.finpro.application.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmf.finpro.application.auth.AddressCommand;
import com.lmf.finpro.application.auth.AuthResult;
import com.lmf.finpro.domain.exception.DocumentAlreadyInUseException;
import com.lmf.finpro.domain.exception.EmailAlreadyInUseException;
import com.lmf.finpro.domain.exception.IncorrectCurrentPasswordException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.BrazilianState;
import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.model.TaxRegime;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.PasswordHasherPort;
import com.lmf.finpro.domain.port.out.TokenPort;
import com.lmf.finpro.domain.port.out.UserRepositoryPort;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileApplicationServiceTest {

    @Mock private UserRepositoryPort userRepositoryPort;
    @Mock private PasswordHasherPort passwordHasherPort;
    @Mock private TokenPort tokenPort;

    @InjectMocks private ProfileApplicationService service;

    private static User existingUser() {
        return new User(
                1L,
                "Ana Freelancer",
                "ana@finpro.test",
                "hashed-password",
                DocumentType.CPF,
                "52998224725",
                "11987654321",
                TaxRegime.AUTONOMO,
                null,
                LocalDateTime.now(),
                2);
    }

    private static UpdateProfileCommand command(
            String email, DocumentType type, String document, TaxRegime regime, String password) {
        return new UpdateProfileCommand(
                "Ana Souza",
                email,
                type,
                document,
                "(11) 91234-5678",
                regime,
                new AddressCommand(
                        "01310-100",
                        "Avenida Paulista",
                        "1000",
                        null,
                        "Bela Vista",
                        "São Paulo",
                        BrazilianState.SP),
                password);
    }

    @Test
    void updateProfileKeepingEmailDoesNotRequirePasswordAndKeepsSessionVersion() {
        when(userRepositoryPort.findById(1L)).thenReturn(Optional.of(existingUser()));
        when(userRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        User updated =
                service.updateProfile(
                        1L,
                        command(
                                "ana@finpro.test",
                                DocumentType.CPF,
                                "529.982.247-25",
                                TaxRegime.AUTONOMO,
                                null));

        assertThat(updated.name()).isEqualTo("Ana Souza");
        assertThat(updated.phone()).isEqualTo("11912345678");
        assertThat(updated.address().zipCode()).isEqualTo("01310100");
        assertThat(updated.passwordHash()).isEqualTo("hashed-password");
        assertThat(updated.sessionVersion()).isEqualTo(2);
        verify(passwordHasherPort, never()).matches(anyString(), anyString());
    }

    @Test
    void changingEmailRequiresCurrentPassword() {
        when(userRepositoryPort.findById(1L)).thenReturn(Optional.of(existingUser()));

        assertThatThrownBy(
                        () ->
                                service.updateProfile(
                                        1L,
                                        command(
                                                "novo@finpro.test",
                                                DocumentType.CPF,
                                                "52998224725",
                                                TaxRegime.AUTONOMO,
                                                null)))
                .isInstanceOf(IncorrectCurrentPasswordException.class)
                .hasMessageContaining("senha atual");
        verify(userRepositoryPort, never()).save(any());
    }

    @Test
    void changingEmailWithWrongPasswordIsRejected() {
        when(userRepositoryPort.findById(1L)).thenReturn(Optional.of(existingUser()));
        when(passwordHasherPort.matches("errada", "hashed-password")).thenReturn(false);

        assertThatThrownBy(
                        () ->
                                service.updateProfile(
                                        1L,
                                        command(
                                                "novo@finpro.test",
                                                DocumentType.CPF,
                                                "52998224725",
                                                TaxRegime.AUTONOMO,
                                                "errada")))
                .isInstanceOf(IncorrectCurrentPasswordException.class);
    }

    @Test
    void changingEmailToOneAlreadyInUseIsRejected() {
        when(userRepositoryPort.findById(1L)).thenReturn(Optional.of(existingUser()));
        when(passwordHasherPort.matches("senha12345", "hashed-password")).thenReturn(true);
        when(userRepositoryPort.existsByEmail("outro@finpro.test")).thenReturn(true);

        assertThatThrownBy(
                        () ->
                                service.updateProfile(
                                        1L,
                                        command(
                                                "outro@finpro.test",
                                                DocumentType.CPF,
                                                "52998224725",
                                                TaxRegime.AUTONOMO,
                                                "senha12345")))
                .isInstanceOf(EmailAlreadyInUseException.class);
    }

    @Test
    void changingDocumentToOneAlreadyInUseIsRejected() {
        when(userRepositoryPort.findById(1L)).thenReturn(Optional.of(existingUser()));
        when(userRepositoryPort.existsByDocumentNumber("11222333000181")).thenReturn(true);

        assertThatThrownBy(
                        () ->
                                service.updateProfile(
                                        1L,
                                        command(
                                                "ana@finpro.test",
                                                DocumentType.CNPJ,
                                                "11.222.333/0001-81",
                                                TaxRegime.MEI,
                                                null)))
                .isInstanceOf(DocumentAlreadyInUseException.class);
    }

    @Test
    void changePasswordBumpsSessionVersionAndReturnsFreshToken() {
        when(userRepositoryPort.findById(1L)).thenReturn(Optional.of(existingUser()));
        when(passwordHasherPort.matches("senha12345", "hashed-password")).thenReturn(true);
        when(passwordHasherPort.hash("novaSenha123")).thenReturn("new-hash");
        when(userRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenPort.generate(1L, "ana@finpro.test", 3)).thenReturn("fresh-token");

        AuthResult result = service.changePassword(1L, "senha12345", "novaSenha123");

        assertThat(result.token()).isEqualTo("fresh-token");
    }

    @Test
    void changePasswordWithWrongCurrentPasswordIsRejected() {
        when(userRepositoryPort.findById(1L)).thenReturn(Optional.of(existingUser()));
        when(passwordHasherPort.matches("errada", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(1L, "errada", "novaSenha123"))
                .isInstanceOf(IncorrectCurrentPasswordException.class);
        verify(userRepositoryPort, never()).save(any());
    }

    @Test
    void getProfileOfMissingUserThrowsNotFound() {
        when(userRepositoryPort.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfile(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
