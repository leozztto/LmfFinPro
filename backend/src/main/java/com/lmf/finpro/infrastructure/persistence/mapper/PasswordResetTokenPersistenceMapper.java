package com.lmf.finpro.infrastructure.persistence.mapper;

import com.lmf.finpro.domain.model.PasswordResetToken;
import com.lmf.finpro.infrastructure.persistence.entity.PasswordResetTokenJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetTokenPersistenceMapper {

    public PasswordResetTokenJpaEntity toEntity(PasswordResetToken token) {
        return PasswordResetTokenJpaEntity.builder()
                .id(token.id())
                .userId(token.userId())
                .tokenHash(token.tokenHash())
                .expiresAt(token.expiresAt())
                .usedAt(token.usedAt())
                .createdAt(token.createdAt())
                .build();
    }

    public PasswordResetToken toDomain(PasswordResetTokenJpaEntity entity) {
        return new PasswordResetToken(
                entity.getId(),
                entity.getUserId(),
                entity.getTokenHash(),
                entity.getExpiresAt(),
                entity.getUsedAt(),
                entity.getCreatedAt());
    }
}
