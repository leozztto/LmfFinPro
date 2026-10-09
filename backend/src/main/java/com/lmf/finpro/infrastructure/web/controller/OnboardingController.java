package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.onboarding.OnboardingApplicationService;
import com.lmf.finpro.domain.model.OnboardingProgress.StepId;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.onboarding.ActivationEmailsRequest;
import com.lmf.finpro.infrastructure.web.dto.onboarding.OnboardingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingApplicationService onboardingApplicationService;

    @GetMapping
    public OnboardingResponse get(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return OnboardingResponse.from(onboardingApplicationService.get(currentUser.userId()));
    }

    @PostMapping("/steps/{step}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void completeStep(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable StepId step) {
        onboardingApplicationService.completeStep(currentUser.userId(), step);
    }

    @PostMapping("/dismiss")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void dismiss(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        onboardingApplicationService.dismiss(currentUser.userId());
    }

    @PutMapping("/activation-emails")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setActivationEmails(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestBody @jakarta.validation.Valid ActivationEmailsRequest request) {
        onboardingApplicationService.setActivationEmailsEnabled(
                currentUser.userId(), request.enabled());
    }
}
