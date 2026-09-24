package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.model.PasswordResetToken;
import com.lmf.finpro.domain.port.out.PasswordResetTokenRepositoryPort;
import com.lmf.finpro.infrastructure.persistence.mapper.PasswordResetTokenPersistenceMapper;
import com.lmf.finpro.infrastructure.persistence.repository.PasswordResetTokenJpaRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordResetTokenRepositoryAdapter implements PasswordResetTokenRepositoryPort {

    private final PasswordResetTokenJpaRepository jpaRepository;
    private final PasswordResetTokenPersistenceMapper mapper;

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(token)));
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public void invalidateActiveTokens(Long userId, LocalDateTime usedAt) {
        jpaRepository.invalidateActiveTokens(userId, usedAt);
    }
}
