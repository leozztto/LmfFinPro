package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.model.RefreshToken;
import com.lmf.finpro.domain.port.out.RefreshTokenRepositoryPort;
import com.lmf.finpro.infrastructure.persistence.mapper.RefreshTokenPersistenceMapper;
import com.lmf.finpro.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepositoryPort {

    private final RefreshTokenJpaRepository jpaRepository;
    private final RefreshTokenPersistenceMapper mapper;

    @Override
    public RefreshToken save(RefreshToken token) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(token)));
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void revokeFamily(String familyId, LocalDateTime revokedAt) {
        jpaRepository.revokeFamily(familyId, revokedAt);
    }

    @Override
    @Transactional
    public int deleteExpiredBefore(LocalDateTime threshold) {
        return jpaRepository.deleteExpiredBefore(threshold);
    }
}
