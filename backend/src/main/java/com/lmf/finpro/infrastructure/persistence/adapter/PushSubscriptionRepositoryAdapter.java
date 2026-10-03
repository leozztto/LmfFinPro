package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.model.PushSubscription;
import com.lmf.finpro.domain.port.out.PushSubscriptionRepositoryPort;
import com.lmf.finpro.infrastructure.persistence.entity.PushSubscriptionJpaEntity;
import com.lmf.finpro.infrastructure.persistence.repository.PushSubscriptionJpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PushSubscriptionRepositoryAdapter implements PushSubscriptionRepositoryPort {

    private final PushSubscriptionJpaRepository repository;

    @Override
    public void upsert(PushSubscription subscription) {
        PushSubscriptionJpaEntity entity =
                repository
                        .findByEndpoint(subscription.endpoint())
                        .orElseGet(
                                () ->
                                        PushSubscriptionJpaEntity.builder()
                                                .endpoint(subscription.endpoint())
                                                .createdAt(LocalDateTime.now())
                                                .build());
        entity.setUserId(subscription.userId());
        entity.setP256dh(subscription.p256dh());
        entity.setAuth(subscription.auth());
        repository.save(entity);
    }

    @Override
    public List<PushSubscription> findAllByUserId(Long userId) {
        return repository.findAllByUserId(userId).stream()
                .map(
                        e ->
                                new PushSubscription(
                                        e.getUserId(), e.getEndpoint(), e.getP256dh(), e.getAuth()))
                .toList();
    }

    @Override
    public void deleteByUserIdAndEndpoint(Long userId, String endpoint) {
        repository.deleteByUserIdAndEndpoint(userId, endpoint);
    }
}
