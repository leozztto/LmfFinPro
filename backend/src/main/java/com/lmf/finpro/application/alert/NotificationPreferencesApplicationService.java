package com.lmf.finpro.application.alert;

import com.lmf.finpro.domain.model.NotificationPreferences;
import com.lmf.finpro.domain.port.out.NotificationPreferencesRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationPreferencesApplicationService {

    private final NotificationPreferencesRepositoryPort notificationPreferencesRepositoryPort;

    public NotificationPreferences get(Long currentUserId) {
        return notificationPreferencesRepositoryPort
                .findByUserId(currentUserId)
                .orElseGet(() -> NotificationPreferences.defaults(currentUserId));
    }

    public NotificationPreferences update(
            Long currentUserId,
            boolean billsEnabled,
            int billDaysBefore,
            boolean budgetsEnabled,
            boolean dasEnabled) {
        return notificationPreferencesRepositoryPort.save(
                new NotificationPreferences(
                        currentUserId, billsEnabled, billDaysBefore, budgetsEnabled, dasEnabled));
    }
}
