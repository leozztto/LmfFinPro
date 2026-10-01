package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.RefreshToken;
import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepositoryPort {
    RefreshToken save(RefreshToken token);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Revoga todos os tokens ainda ativos da cadeia de rotações (reuso detectado ou logout). */
    void revokeFamily(String familyId, LocalDateTime revokedAt);

    /** Remove tokens expirados (limpeza periódica). */
    int deleteExpiredBefore(LocalDateTime threshold);
}
