package com.lmf.finpro.application.auth;

import com.lmf.finpro.application.support.SecureTokens;
import com.lmf.finpro.domain.exception.InvalidPasswordResetTokenException;
import com.lmf.finpro.domain.model.PasswordResetToken;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.PasswordHasherPort;
import com.lmf.finpro.domain.port.out.PasswordResetMailerPort;
import com.lmf.finpro.domain.port.out.PasswordResetTokenRepositoryPort;
import com.lmf.finpro.domain.port.out.UserRepositoryPort;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fluxo "esqueci minha senha": gera um token aleatório de uso único, envia o link por e-mail e,
 * depois, troca a senha de quem apresentar um token válido.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetApplicationService {

    private static final String INVALID_TOKEN_MESSAGE =
            "Link de redefinição inválido ou expirado. Solicite um novo.";

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordResetTokenRepositoryPort tokenRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;
    private final PasswordResetMailerPort mailerPort;
    private final PasswordResetSettings settings;

    /**
     * Não informa se o e-mail existe: quem chama recebe sempre a mesma resposta, senão o endpoint
     * viraria uma forma de descobrir quais e-mails têm conta no FinPro.
     */
    @Transactional
    public void requestReset(String email) {
        log.debug("Solicitação de redefinição de senha recebida");
        userRepositoryPort.findByEmail(email).ifPresent(this::issueAndSendToken);
    }

    @Transactional
    public void resetPassword(String rawToken, String newRawPassword) {
        log.debug("Redefinindo senha com token de redefinição");
        LocalDateTime now = LocalDateTime.now();
        PasswordResetToken token =
                tokenRepositoryPort
                        .findByTokenHash(SecureTokens.sha256(rawToken))
                        .filter(t -> t.isUsable(now))
                        .orElseThrow(
                                () ->
                                        new InvalidPasswordResetTokenException(
                                                INVALID_TOKEN_MESSAGE));

        User user =
                userRepositoryPort
                        .findById(token.userId())
                        .orElseThrow(
                                () ->
                                        new InvalidPasswordResetTokenException(
                                                INVALID_TOKEN_MESSAGE));

        userRepositoryPort.save(user.withPasswordHash(passwordHasherPort.hash(newRawPassword)));
        tokenRepositoryPort.save(token.markUsed(now));
        log.info("Senha redefinida para o usuário={}", user.id());
    }

    private void issueAndSendToken(User user) {
        LocalDateTime now = LocalDateTime.now();
        tokenRepositoryPort.invalidateActiveTokens(user.id(), now);

        String rawToken = SecureTokens.generate();
        tokenRepositoryPort.save(
                PasswordResetToken.issue(
                        user.id(), SecureTokens.sha256(rawToken), now, settings.tokenTtlMinutes()));

        String link = settings.resetPageUrl() + "?token=" + rawToken;
        try {
            mailerPort.sendResetLink(user.email(), user.name(), link, settings.tokenTtlMinutes());
        } catch (RuntimeException ex) {
            // Falha no envio não pode virar erro na resposta: diria ao chamador que o e-mail
            // existe.
            // Só os tipos da exceção: a mensagem (e o stack trace com ela) de erros de SMTP costuma
            // trazer o endereço do destinatário.
            log.error(
                    "Falha ao enviar e-mail de redefinição de senha (userId={}, erro={}, causa={})",
                    user.id(),
                    ex.getClass().getSimpleName(),
                    rootCause(ex).getClass().getSimpleName());
        }
    }

    private static Throwable rootCause(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }
}
