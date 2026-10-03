package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.PushSubscriptionJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushSubscriptionJpaRepository
        extends JpaRepository<PushSubscriptionJpaEntity, Long> {

    Optional<PushSubscriptionJpaEntity> findByEndpoint(String endpoint);

    List<PushSubscriptionJpaEntity> findAllByUserId(Long userId);

    void deleteByUserIdAndEndpoint(Long userId, String endpoint);
}
