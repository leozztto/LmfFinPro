package com.lmf.finpro.application.alert;

import com.lmf.finpro.domain.model.NotificationPreferences;
import com.lmf.finpro.domain.port.out.NotificationPreferencesRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPreferencesApplicationService {

    private final NotificationPreferencesRepositoryPort notificationPreferencesRepositoryPort;

    public NotificationPreferences get(Long currentUserId) {
        log.debug("Buscando preferências de notificação do usuário={}", currentUserId);
        return notificationPreferencesRepositoryPort
                .findByUserId(currentUserId)
                .orElseGet(() -> NotificationPreferences.defaults(currentUserId));
    }

    public NotificationPreferences update(
            Long currentUserId,
            boolean billsEnabled,
            int billDaysBefore,
            boolean budgetsEnabled,
            boolean dasEnabled,
            boolean recurringBudgetsEnabled) {
        log.debug("Atualizando preferências de notificação do usuário={}", currentUserId);
        return notificationPreferencesRepositoryPort.save(
                new NotificationPreferences(
                        currentUserId,
                        billsEnabled,
                        billDaysBefore,
                        budgetsEnabled,
                        dasEnabled,
                        recurringBudgetsEnabled));
    }
}
