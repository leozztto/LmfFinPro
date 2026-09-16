package com.lmf.finpro.application.auth;

import com.lmf.finpro.domain.exception.EmailAlreadyInUseException;
import com.lmf.finpro.domain.exception.InvalidCredentialsException;
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

        String passwordHash = passwordHasherPort.hash(command.rawPassword());
        User saved = userRepositoryPort.save(
            User.register(command.name(), command.email(), passwordHash, command.taxRegime())
        );

        return toAuthResult(saved);
    }

    public AuthResult login(LoginCommand command) {
        User user = userRepositoryPort.findByEmail(command.email())
            .orElseThrow(() -> new InvalidCredentialsException("E-mail ou senha inválidos"));

        if (!passwordHasherPort.matches(command.rawPassword(), user.passwordHash())) {
            throw new InvalidCredentialsException("E-mail ou senha inválidos");
        }

        return toAuthResult(user);
    }

    private AuthResult toAuthResult(User user) {
        String token = tokenPort.generate(user.id(), user.email());
        return new AuthResult(token, user.id(), user.name(), user.email());
    }
}
