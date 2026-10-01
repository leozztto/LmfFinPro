package com.lmf.finpro.infrastructure.persistence.mapper;

import com.lmf.finpro.domain.model.RefreshToken;
import com.lmf.finpro.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenPersistenceMapper {

    public RefreshTokenJpaEntity toEntity(RefreshToken token) {
        return RefreshTokenJpaEntity.builder()
                .id(token.id())
                .userId(token.userId())
                .tokenHash(token.tokenHash())
                .familyId(token.familyId())
                .sessionVersion(token.sessionVersion())
                .expiresAt(token.expiresAt())
                .revokedAt(token.revokedAt())
                .createdAt(token.createdAt())
                .build();
    }

    public RefreshToken toDomain(RefreshTokenJpaEntity entity) {
        return new RefreshToken(
                entity.getId(),
                entity.getUserId(),
                entity.getTokenHash(),
                entity.getFamilyId(),
                entity.getSessionVersion(),
                entity.getExpiresAt(),
                entity.getRevokedAt(),
                entity.getCreatedAt());
    }
}
