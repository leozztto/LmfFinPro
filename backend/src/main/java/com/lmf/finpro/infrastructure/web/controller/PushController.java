package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.push.PushNotificationApplicationService;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.push.PushConfigResponse;
import com.lmf.finpro.infrastructure.web.dto.push.PushSubscriptionRequest;
import com.lmf.finpro.infrastructure.web.dto.push.PushUnsubscribeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Inscrição do navegador do usuário nas notificações push (PWA). */
@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushController {

    private final PushNotificationApplicationService pushNotificationApplicationService;

    @GetMapping("/config")
    public PushConfigResponse config() {
        return pushNotificationApplicationService
                .publicKey()
                .map(key -> new PushConfigResponse(true, key))
                .orElseGet(() -> new PushConfigResponse(false, null));
    }

    @PostMapping("/subscriptions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void subscribe(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody PushSubscriptionRequest request) {
        pushNotificationApplicationService.subscribe(
                currentUser.userId(), request.endpoint(), request.p256dh(), request.auth());
    }

    @DeleteMapping("/subscriptions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody PushUnsubscribeRequest request) {
        pushNotificationApplicationService.unsubscribe(currentUser.userId(), request.endpoint());
    }
}
