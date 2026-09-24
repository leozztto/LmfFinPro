package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.PasswordResetToken;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepositoryPort {
    PasswordResetToken save(PasswordResetToken token);

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** Marca como usados todos os tokens ainda não usados do usuário (só o último link vale). */
    void invalidateActiveTokens(Long userId, LocalDateTime usedAt);
}
