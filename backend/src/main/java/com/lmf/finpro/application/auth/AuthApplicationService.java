package com.lmf.finpro.application.auth;

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

    public AuthResult register(RegisterCommand command) {
        if (userRepositoryPort.existsByEmail(command.email())) {
            throw new EmailAlreadyInUseException("Já existe uma conta cadastrada com este e-mail");
        }

        String documentNumber = onlyDigits(command.documentNumber());
        if (userRepositoryPort.existsByDocumentNumber(documentNumber)) {
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
                                () -> new InvalidCredentialsException("E-mail ou senha inválidos"));

        if (!passwordHasherPort.matches(command.rawPassword(), user.passwordHash())) {
            throw new InvalidCredentialsException("E-mail ou senha inválidos");
        }

        return toAuthResult(user);
    }

    private Address toAddress(AddressCommand address) {
        if (address == null) {
            return null;
        }
        return new Address(
                onlyDigits(address.zipCode()),
                address.street(),
                address.number(),
                address.complement(),
                address.neighborhood(),
                address.city(),
                address.state());
    }

    private AuthResult toAuthResult(User user) {
        String token = tokenPort.generate(user.id(), user.email(), user.sessionVersion());
        return new AuthResult(token, user.id(), user.name(), user.email());
    }

    private String onlyDigits(String value) {
        return value == null ? null : value.replaceAll("\\D", "");
    }
}
