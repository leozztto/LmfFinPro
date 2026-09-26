package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.model.NotificationPreferences;
import com.lmf.finpro.domain.port.out.NotificationPreferencesRepositoryPort;
import com.lmf.finpro.infrastructure.persistence.entity.NotificationPreferencesJpaEntity;
import com.lmf.finpro.infrastructure.persistence.repository.NotificationPreferencesJpaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationPreferencesRepositoryAdapter
        implements NotificationPreferencesRepositoryPort {

    private final NotificationPreferencesJpaRepository repository;

    @Override
    public NotificationPreferences save(NotificationPreferences preferences) {
        return toDomain(
                repository.save(
                        NotificationPreferencesJpaEntity.builder()
                                .userId(preferences.userId())
                                .billsEnabled(preferences.billsEnabled())
                                .billDaysBefore(preferences.billDaysBefore())
                                .budgetsEnabled(preferences.budgetsEnabled())
                                .dasEnabled(preferences.dasEnabled())
                                .build()));
    }

    @Override
    public Optional<NotificationPreferences> findByUserId(Long userId) {
        return repository.findById(userId).map(this::toDomain);
    }

    private NotificationPreferences toDomain(NotificationPreferencesJpaEntity entity) {
        return new NotificationPreferences(
                entity.getUserId(),
                entity.isBillsEnabled(),
                entity.getBillDaysBefore(),
                entity.isBudgetsEnabled(),
                entity.isDasEnabled());
    }
}
