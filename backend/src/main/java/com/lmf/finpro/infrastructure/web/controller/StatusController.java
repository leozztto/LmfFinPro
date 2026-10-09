package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.status.PlatformStatusService;
import com.lmf.finpro.infrastructure.web.dto.status.PlatformStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Público: a página de suporte mostra a situação da plataforma mesmo para quem não consegue entrar.
 */
@RestController
@RequestMapping("/api/status")
@RequiredArgsConstructor
public class StatusController {

    private final PlatformStatusService platformStatusService;

    @GetMapping
    public ResponseEntity<PlatformStatusResponse> status() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(PlatformStatusResponse.from(platformStatusService.check()));
    }
}
