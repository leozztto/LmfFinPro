package com.lmf.finpro.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.exception.DocumentAlreadyInUseException;
import com.lmf.finpro.domain.exception.EmailAlreadyInUseException;
import com.lmf.finpro.domain.exception.InvalidCredentialsException;
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
class AuthApplicationServiceTest {

    @Mock private UserRepositoryPort userRepositoryPort;
    @Mock private PasswordHasherPort passwordHasherPort;
    @Mock private TokenPort tokenPort;

    @InjectMocks private AuthApplicationService service;

    private static AddressCommand sampleAddress() {
        return new AddressCommand(
                "013.10-100",
                "Avenida Paulista",
                "1000",
                null,
                "Bela Vista",
                "São Paulo",
                BrazilianState.SP);
    }

    private static RegisterCommand sampleCommand() {
        return new RegisterCommand(
                "Ana Freelancer",
                "ana@finpro.test",
                "senha12345",
                DocumentType.CPF,
                "529.982.247-25",
                "(11) 98765-4321",
                TaxRegime.AUTONOMO,
                sampleAddress());
    }

    @Test
    void registerHashesPasswordAndStripsNonDigitsFromDocumentAndPhone() {
        when(userRepositoryPort.existsByEmail(any())).thenReturn(false);
        when(userRepositoryPort.existsByDocumentNumber("52998224725")).thenReturn(false);
        when(passwordHasherPort.hash("senha12345")).thenReturn("hashed-password");
        when(userRepositoryPort.save(any()))
                .thenAnswer(
                        invocation -> {
                            User toSave = invocation.getArgument(0);
                            return new User(
                                    1L,
                                    toSave.name(),
                                    toSave.email(),
                                    toSave.passwordHash(),
                                    toSave.documentType(),
                                    toSave.documentNumber(),
                                    toSave.phone(),
                                    toSave.taxRegime(),
                                    toSave.address(),
                                    LocalDateTime.now());
                        });
        when(tokenPort.generate(1L, "ana@finpro.test")).thenReturn("jwt-token");

        AuthResult result = service.register(sampleCommand());

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.userId()).isEqualTo(1L);
        verify(passwordHasherPort).hash("senha12345");
        verify(userRepositoryPort).existsByDocumentNumber("52998224725");
    }

    @Test
    void registerAcceptsANullAddress() {
        RegisterCommand commandWithoutAddress =
                new RegisterCommand(
                        "Ana Freelancer",
                        "ana@finpro.test",
                        "senha12345",
                        DocumentType.CPF,
                        "529.982.247-25",
                        "(11) 98765-4321",
                        TaxRegime.AUTONOMO,
                        null);
        when(userRepositoryPort.existsByEmail(any())).thenReturn(false);
        when(userRepositoryPort.existsByDocumentNumber("52998224725")).thenReturn(false);
        when(passwordHasherPort.hash("senha12345")).thenReturn("hashed-password");
        when(userRepositoryPort.save(any()))
                .thenAnswer(
                        invocation -> {
                            User toSave = invocation.getArgument(0);
                            return new User(
                                    1L,
                                    toSave.name(),
                                    toSave.email(),
                                    toSave.passwordHash(),
                                    toSave.documentType(),
                                    toSave.documentNumber(),
                                    toSave.phone(),
                                    toSave.taxRegime(),
                                    toSave.address(),
                                    LocalDateTime.now());
                        });
        when(tokenPort.generate(1L, "ana@finpro.test")).thenReturn("jwt-token");

        AuthResult result = service.register(commandWithoutAddress);

        assertThat(result.token()).isEqualTo("jwt-token");
    }

    @Test
    void registerThrowsWhenEmailAlreadyInUse() {
        when(userRepositoryPort.existsByEmail("ana@finpro.test")).thenReturn(true);

        assertThatThrownBy(() -> service.register(sampleCommand()))
                .isInstanceOf(EmailAlreadyInUseException.class);
    }

    @Test
    void registerThrowsWhenDocumentAlreadyInUse() {
        when(userRepositoryPort.existsByEmail(any())).thenReturn(false);
        when(userRepositoryPort.existsByDocumentNumber(eq("52998224725"))).thenReturn(true);

        assertThatThrownBy(() -> service.register(sampleCommand()))
                .isInstanceOf(DocumentAlreadyInUseException.class);
    }

    @Test
    void loginReturnsTokenWhenCredentialsAreValid() {
        User user =
                new User(
                        1L,
                        "Ana",
                        "ana@finpro.test",
                        "hashed-password",
                        DocumentType.CPF,
                        "52998224725",
                        "11987654321",
                        TaxRegime.AUTONOMO,
                        null,
                        LocalDateTime.now());
        when(userRepositoryPort.findByEmail("ana@finpro.test")).thenReturn(Optional.of(user));
        when(passwordHasherPort.matches("senha12345", "hashed-password")).thenReturn(true);
        when(tokenPort.generate(1L, "ana@finpro.test")).thenReturn("jwt-token");

        AuthResult result = service.login(new LoginCommand("ana@finpro.test", "senha12345"));

        assertThat(result.token()).isEqualTo("jwt-token");
    }

    @Test
    void loginThrowsWhenEmailNotFound() {
        when(userRepositoryPort.findByEmail("missing@finpro.test")).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () -> service.login(new LoginCommand("missing@finpro.test", "senha12345")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginThrowsWhenPasswordDoesNotMatch() {
        User user =
                new User(
                        1L,
                        "Ana",
                        "ana@finpro.test",
                        "hashed-password",
                        DocumentType.CPF,
                        "52998224725",
                        "11987654321",
                        TaxRegime.AUTONOMO,
                        null,
                        LocalDateTime.now());
        when(userRepositoryPort.findByEmail("ana@finpro.test")).thenReturn(Optional.of(user));
        when(passwordHasherPort.matches("wrong", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginCommand("ana@finpro.test", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
