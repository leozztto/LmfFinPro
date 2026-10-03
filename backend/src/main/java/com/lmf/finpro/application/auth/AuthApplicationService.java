package com.lmf.finpro.application.auth;

import com.lmf.finpro.application.FlowLog;
import com.lmf.finpro.domain.exception.DocumentAlreadyInUseException;
import com.lmf.finpro.domain.exception.EmailAlreadyInUseException;
import com.lmf.finpro.domain.exception.InvalidCredentialsException;
import com.lmf.finpro.domain.model.Address;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.PasswordHasherPort;
import com.lmf.finpro.domain.port.out.TokenPort;
import com.lmf.finpro.domain.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthApplicationService {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;
    private final TokenPort tokenPort;
    private final RefreshTokenApplicationService refreshTokenApplicationService;

    public AuthResult register(RegisterCommand command) {
        if (userRepositoryPort.existsByEmail(command.email())) {
            FlowLog.detail("reason", "emailInUse");
            throw new EmailAlreadyInUseException("Já existe uma conta cadastrada com este e-mail");
        }

        String documentNumber = onlyDigits(command.documentNumber());
        if (userRepositoryPort.existsByDocumentNumber(documentNumber)) {
            FlowLog.detail("reason", "documentInUse");
            throw new DocumentAlreadyInUseException(
                    "Já existe uma conta cadastrada com este " + command.documentType());
        }

        String passwordHash = passwordHasherPort.hash(command.rawPassword());
        User saved =
                userRepositoryPort.save(
                        User.register(
                                command.name(),
                                command.email(),
                                passwordHash,
                                command.documentType(),
                                documentNumber,
                                onlyDigits(command.phone()),
                                command.taxRegime(),
                                toAddress(command.address())));

        return toAuthResult(saved);
    }

    public AuthResult login(LoginCommand command) {
        User user =
                userRepositoryPort
                        .findByEmail(command.email())
                        .orElseThrow(
                                () -> {
                                    FlowLog.detail("reason", "unknownEmail");
                                    return new InvalidCredentialsException(
                                            "E-mail ou senha inválidos");
                                });

        if (!passwordHasherPort.matches(command.rawPassword(), user.passwordHash())) {
            FlowLog.detail("reason", "wrongPassword");
            FlowLog.detail("userId", user.id());
            throw new InvalidCredentialsException("E-mail ou senha inválidos");
        }

        return toAuthResult(user);
    }

    private Address toAddress(AddressCommand address) {
        return address == null ? null : address.toDomain();
    }

    private AuthResult toAuthResult(User user) {
        FlowLog.detail("userId", user.id());
        String token = tokenPort.generate(user.id(), user.email(), user.sessionVersion());
        String refreshToken = refreshTokenApplicationService.startSession(user);
        return new AuthResult(token, refreshToken, user.id(), user.name(), user.email());
    }

    private String onlyDigits(String value) {
        return value == null ? null : value.replaceAll("\\D", "");
    }
}
