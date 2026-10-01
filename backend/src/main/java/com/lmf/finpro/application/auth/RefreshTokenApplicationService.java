package com.lmf.finpro.application.auth;

import com.lmf.finpro.domain.exception.InvalidTokenException;
import com.lmf.finpro.domain.model.RefreshToken;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.RefreshTokenRepositoryPort;
import com.lmf.finpro.domain.port.out.TokenPort;
import com.lmf.finpro.domain.port.out.UserRepositoryPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sessão de longa duração: o refresh token é um valor aleatório opaco (só o hash vai para o banco)
 * que o navegador guarda em cookie httpOnly e troca por um access token JWT curto. A cada uso ele é
 * rotacionado; apresentar de novo um token já rotacionado indica roubo e derruba a cadeia toda.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenApplicationService {

    private static final String INVALID_MESSAGE = "Sessão expirada. Faça login novamente.";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final TokenPort tokenPort;
    private final RefreshTokenSettings settings;
    private final Clock clock;

    /** Abre uma sessão nova (login, cadastro, troca de senha) e devolve o valor para o cookie. */
    @Transactional
    public String startSession(User user) {
        return issue(user.id(), user.sessionVersion(), UUID.randomUUID().toString());
    }

    /**
     * Troca o refresh token por um access token novo. O {@code refreshToken} do resultado é null
     * quando a chamada caiu na tolerância de concorrência: o navegador já recebeu o token novo pela
     * outra requisição, então o cookie não deve ser sobrescrito com um valor mais antigo.
     */
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public AuthResult refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidTokenException(INVALID_MESSAGE);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        RefreshToken current =
                refreshTokenRepositoryPort
                        .findByTokenHash(sha256(rawToken))
                        .orElseThrow(() -> new InvalidTokenException(INVALID_MESSAGE));

        boolean withinLeeway = false;
        if (current.isRevoked()) {
            withinLeeway = isWithinLeeway(current, now);
            if (!withinLeeway) {
                log.warn(
                        "Reuso de refresh token já rotacionado; sessão encerrada (userId={})",
                        current.userId());
                refreshTokenRepositoryPort.revokeFamily(current.familyId(), now);
                throw new InvalidTokenException(INVALID_MESSAGE);
            }
        }
        if (current.isExpired(now)) {
            throw new InvalidTokenException(INVALID_MESSAGE);
        }

        User user =
                userRepositoryPort
                        .findById(current.userId())
                        .filter(u -> u.sessionVersion() == current.sessionVersion())
                        .orElse(null);
        if (user == null) {
            // Senha trocada (versão de sessão mudou) ou usuário excluído.
            refreshTokenRepositoryPort.revokeFamily(current.familyId(), now);
            throw new InvalidTokenException(INVALID_MESSAGE);
        }

        String accessToken = tokenPort.generate(user.id(), user.email(), user.sessionVersion());
        if (withinLeeway) {
            return new AuthResult(accessToken, null, user.id(), user.name(), user.email());
        }

        refreshTokenRepositoryPort.save(current.revoke(now));
        String nextRefresh = issue(user.id(), user.sessionVersion(), current.familyId());
        return new AuthResult(accessToken, nextRefresh, user.id(), user.name(), user.email());
    }

    /**
     * Encerra a sessão do cookie apresentado; token desconhecido é ignorado (logout idempotente).
     */
    @Transactional
    public void endSession(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepositoryPort
                .findByTokenHash(sha256(rawToken))
                .ifPresent(
                        token ->
                                refreshTokenRepositoryPort.revokeFamily(
                                        token.familyId(), LocalDateTime.now(clock)));
    }

    /** Apaga tokens expirados; devolve quantos foram removidos. */
    @Transactional
    public int purgeExpired() {
        return refreshTokenRepositoryPort.deleteExpiredBefore(LocalDateTime.now(clock));
    }

    private boolean isWithinLeeway(RefreshToken token, LocalDateTime now) {
        return Duration.between(token.revokedAt(), now).getSeconds()
                < settings.reuseLeewaySeconds();
    }

    private String issue(Long userId, int sessionVersion, String familyId) {
        String raw = generateRawToken();
        refreshTokenRepositoryPort.save(
                RefreshToken.issue(
                        userId,
                        sha256(raw),
                        familyId,
                        sessionVersion,
                        LocalDateTime.now(clock),
                        settings.ttlDays()));
        return raw;
    }

    private static String generateRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            byte[] digest =
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", ex);
        }
    }
}
