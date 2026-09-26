package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.NotificationPreferences;
import java.util.Optional;

public interface NotificationPreferencesRepositoryPort {
    NotificationPreferences save(NotificationPreferences preferences);

    Optional<NotificationPreferences> findByUserId(Long userId);
}
