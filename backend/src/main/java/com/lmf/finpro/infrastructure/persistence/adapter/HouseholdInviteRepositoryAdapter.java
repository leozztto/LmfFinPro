package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.model.HouseholdInvite;
import com.lmf.finpro.domain.port.out.HouseholdInviteRepositoryPort;
import com.lmf.finpro.infrastructure.persistence.entity.HouseholdInviteJpaEntity;
import com.lmf.finpro.infrastructure.persistence.repository.HouseholdInviteJpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HouseholdInviteRepositoryAdapter implements HouseholdInviteRepositoryPort {

    private final HouseholdInviteJpaRepository jpaRepository;

    @Override
    public HouseholdInvite save(HouseholdInvite invite) {
        return toDomain(
                jpaRepository.save(
                        HouseholdInviteJpaEntity.builder()
                                .id(invite.id())
                                .householdId(invite.householdId())
                                .email(invite.email())
                                .tokenHash(invite.tokenHash())
                                .role(invite.role())
                                .createdBy(invite.createdBy())
                                .expiresAt(invite.expiresAt())
                                .acceptedAt(invite.acceptedAt())
                                .createdAt(invite.createdAt())
                                .build()));
    }

    @Override
    public Optional<HouseholdInvite> findById(Long id) {
        return jpaRepository.findById(id).map(HouseholdInviteRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<HouseholdInvite> findByTokenHash(String tokenHash) {
        return jpaRepository
                .findByTokenHash(tokenHash)
                .map(HouseholdInviteRepositoryAdapter::toDomain);
    }

    @Override
    public List<HouseholdInvite> findPendingByHouseholdId(Long householdId, LocalDateTime now) {
        return jpaRepository.findPending(householdId, now).stream()
                .map(HouseholdInviteRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public List<HouseholdInvite> findPendingByEmail(String email, LocalDateTime now) {
        return jpaRepository.findPendingByEmail(email, now).stream()
                .map(HouseholdInviteRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public void deletePendingByHouseholdIdAndEmail(
            Long householdId, String email, LocalDateTime now) {
        jpaRepository.deletePending(householdId, email, now);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    private static HouseholdInvite toDomain(HouseholdInviteJpaEntity entity) {
        return new HouseholdInvite(
                entity.getId(),
                entity.getHouseholdId(),
                entity.getEmail(),
                entity.getTokenHash(),
                entity.getRole(),
                entity.getCreatedBy(),
                entity.getExpiresAt(),
                entity.getAcceptedAt(),
                entity.getCreatedAt());
    }
}
