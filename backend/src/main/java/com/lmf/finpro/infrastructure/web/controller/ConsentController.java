package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.legal.ConsentApplicationService;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.legal.AcceptConsentRequest;
import com.lmf.finpro.infrastructure.web.dto.legal.ConsentStatusResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Aceite dos Termos de Uso e da Política de Privacidade pelo usuário logado. */
@RestController
@RequestMapping("/api/consents")
@RequiredArgsConstructor
public class ConsentController {

    private final ConsentApplicationService consentApplicationService;

    @GetMapping
    public ConsentStatusResponse status(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ConsentStatusResponse.from(consentApplicationService.status(currentUser.userId()));
    }

    @PostMapping
    public ConsentStatusResponse accept(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody AcceptConsentRequest request) {
        return ConsentStatusResponse.from(
                consentApplicationService.accept(
                        currentUser.userId(), request.termsVersion(), request.privacyVersion()));
    }
}
