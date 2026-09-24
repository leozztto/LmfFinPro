package com.lmf.finpro.application.profile;

import com.lmf.finpro.application.auth.AuthResult;
import com.lmf.finpro.domain.exception.DocumentAlreadyInUseException;
import com.lmf.finpro.domain.exception.EmailAlreadyInUseException;
import com.lmf.finpro.domain.exception.IncorrectCurrentPasswordException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.PasswordHasherPort;
import com.lmf.finpro.domain.port.out.TokenPort;
import com.lmf.finpro.domain.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Dados cadastrais e senha do próprio usuário logado ("Meu perfil"). */
@Service
@RequiredArgsConstructor
public class ProfileApplicationService {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;
    private final TokenPort tokenPort;

    public User getProfile(Long userId) {
        return findUser(userId);
    }

    @Transactional
    public User updateProfile(Long userId, UpdateProfileCommand command) {
        User user = findUser(userId);

        boolean emailChanged = !user.email().equalsIgnoreCase(command.email());
        if (emailChanged) {
            // O e-mail é o login: sem a senha, quem pegasse uma sessão aberta tomaria a conta.
            checkCurrentPassword(
                    user,
                    command.currentPassword(),
                    "Informe sua senha atual para alterar o e-mail");
            if (userRepositoryPort.existsByEmail(command.email())) {
                throw new EmailAlreadyInUseException(
                        "Já existe uma conta cadastrada com este e-mail");
            }
        }

        String documentNumber = onlyDigits(command.documentNumber());
        if (!documentNumber.equals(user.documentNumber())
                && userRepositoryPort.existsByDocumentNumber(documentNumber)) {
            throw new DocumentAlreadyInUseException(
                    "Já existe uma conta cadastrada com este " + command.documentType());
        }

        return userRepositoryPort.save(
                user.withProfile(
                        command.name(),
                        command.email(),
                        command.documentType(),
                        documentNumber,
                        onlyDigits(command.phone()),
                        command.taxRegime(),
                        command.address().toDomain()));
    }

    /**
     * Troca a senha e encerra as outras sessões (ver User#withPasswordHash). Devolve um token novo
     * para a sessão de quem fez a troca continuar valendo.
     */
    @Transactional
    public AuthResult changePassword(Long userId, String currentPassword, String newPassword) {
        User user = findUser(userId);
        checkCurrentPassword(user, currentPassword, "Senha atual incorreta");

        User saved =
                userRepositoryPort.save(
                        user.withPasswordHash(passwordHasherPort.hash(newPassword)));
        String token = tokenPort.generate(saved.id(), saved.email(), saved.sessionVersion());
        return new AuthResult(token, saved.id(), saved.name(), saved.email());
    }

    private void checkCurrentPassword(User user, String rawPassword, String blankMessage) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IncorrectCurrentPasswordException(blankMessage);
        }
        if (!passwordHasherPort.matches(rawPassword, user.passwordHash())) {
            throw new IncorrectCurrentPasswordException("Senha atual incorreta");
        }
    }

    private User findUser(Long userId) {
        return userRepositoryPort
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    private String onlyDigits(String value) {
        return value == null ? null : value.replaceAll("\\D", "");
    }
}
