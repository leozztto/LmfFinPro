package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.model.AlertType;
import com.lmf.finpro.domain.model.SentAlert;
import com.lmf.finpro.domain.port.out.SentAlertRepositoryPort;
import com.lmf.finpro.infrastructure.persistence.entity.SentAlertJpaEntity;
import com.lmf.finpro.infrastructure.persistence.repository.SentAlertJpaRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SentAlertRepositoryAdapter implements SentAlertRepositoryPort {

    private final SentAlertJpaRepository repository;

    @Override
    public boolean exists(Long userId, AlertType type, String referenceKey) {
        return repository.existsByUserIdAndAlertTypeAndReferenceKey(userId, type, referenceKey);
    }

    @Override
    public void save(SentAlert sentAlert) {
        repository.save(
                SentAlertJpaEntity.builder()
                        .userId(sentAlert.userId())
                        .alertType(sentAlert.type())
                        .referenceKey(sentAlert.referenceKey())
                        .sentAt(LocalDateTime.now())
                        .build());
    }
}
