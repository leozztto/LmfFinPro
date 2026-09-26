package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.alert.NotificationPreferencesApplicationService;
import com.lmf.finpro.domain.model.NotificationPreferences;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.profile.NotificationPreferencesRequest;
import com.lmf.finpro.infrastructure.web.dto.profile.NotificationPreferencesResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Preferências dos alertas por e-mail do próprio usuário (fora do PUT /api/profile, que pede
 * senha).
 */
@RestController
@RequestMapping("/api/profile/notifications")
@RequiredArgsConstructor
public class NotificationPreferencesController {

    private final NotificationPreferencesApplicationService
            notificationPreferencesApplicationService;

    @GetMapping
    public NotificationPreferencesResponse get(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return toResponse(notificationPreferencesApplicationService.get(currentUser.userId()));
    }

    @PutMapping
    public NotificationPreferencesResponse update(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody NotificationPreferencesRequest request) {
        return toResponse(
                notificationPreferencesApplicationService.update(
                        currentUser.userId(),
                        request.billsEnabled(),
                        request.billDaysBefore(),
                        request.budgetsEnabled(),
                        request.dasEnabled()));
    }

    private NotificationPreferencesResponse toResponse(NotificationPreferences preferences) {
        return new NotificationPreferencesResponse(
                preferences.billsEnabled(),
                preferences.billDaysBefore(),
                preferences.budgetsEnabled(),
                preferences.dasEnabled());
    }
}
