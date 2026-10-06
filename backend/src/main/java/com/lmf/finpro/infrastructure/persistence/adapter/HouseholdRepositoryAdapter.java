package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.model.Household;
import com.lmf.finpro.domain.model.HouseholdMembership;
import com.lmf.finpro.domain.port.out.HouseholdRepositoryPort;
import com.lmf.finpro.infrastructure.persistence.entity.HouseholdJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.HouseholdMemberJpaEntity;
import com.lmf.finpro.infrastructure.persistence.repository.HouseholdJpaRepository;
import com.lmf.finpro.infrastructure.persistence.repository.HouseholdMemberJpaRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HouseholdRepositoryAdapter implements HouseholdRepositoryPort {

    private final HouseholdJpaRepository householdJpaRepository;
    private final HouseholdMemberJpaRepository memberJpaRepository;

    @Override
    public Household save(Household household) {
        return toDomain(
                householdJpaRepository.save(
                        HouseholdJpaEntity.builder()
                                .id(household.id())
                                .name(household.name())
                                .type(household.type())
                                .createdAt(household.createdAt())
                                .build()));
    }

    @Override
    public Optional<Household> findById(Long id) {
        return householdJpaRepository.findById(id).map(HouseholdRepositoryAdapter::toDomain);
    }

    @Override
    public HouseholdMembership saveMembership(HouseholdMembership membership) {
        // A chave natural é (grupo, usuário): reaproveita a linha existente, por exemplo ao
        // transferir a posse, em vez de criar um segundo vínculo.
        HouseholdMemberJpaEntity entity =
                memberJpaRepository
                        .findByHouseholdIdAndUserId(membership.householdId(), membership.userId())
                        .orElseGet(
                                () ->
                                        HouseholdMemberJpaEntity.builder()
                                                .householdId(membership.householdId())
                                                .userId(membership.userId())
                                                .build());
        entity.setRole(membership.role());
        memberJpaRepository.save(entity);
        return membership;
    }

    @Override
    public void deleteMembership(Long householdId, Long userId) {
        memberJpaRepository.deleteByHouseholdIdAndUserId(householdId, userId);
    }

    @Override
    public Optional<HouseholdMembership> findMembership(Long householdId, Long userId) {
        return memberJpaRepository
                .findByHouseholdIdAndUserId(householdId, userId)
                .map(HouseholdRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<HouseholdMembership> findPersonalMembership(Long userId) {
        return memberJpaRepository
                .findPersonalByUserId(userId)
                .map(HouseholdRepositoryAdapter::toDomain);
    }

    @Override
    public List<HouseholdMembership> findMembershipsByUserId(Long userId) {
        return memberJpaRepository.findByUserId(userId).stream()
                .map(HouseholdRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public List<HouseholdMembership> findMembershipsByHouseholdId(Long householdId) {
        return memberJpaRepository.findByHouseholdIdOrderByJoinedAtAsc(householdId).stream()
                .map(HouseholdRepositoryAdapter::toDomain)
                .toList();
    }

    private static Household toDomain(HouseholdJpaEntity entity) {
        return new Household(
                entity.getId(), entity.getName(), entity.getType(), entity.getCreatedAt());
    }

    private static HouseholdMembership toDomain(HouseholdMemberJpaEntity entity) {
        return new HouseholdMembership(
                entity.getHouseholdId(), entity.getUserId(), entity.getRole());
    }
}
