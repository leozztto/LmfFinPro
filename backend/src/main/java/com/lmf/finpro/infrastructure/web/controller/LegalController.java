package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.legal.LegalDocuments;
import com.lmf.finpro.infrastructure.web.dto.legal.LegalVersionsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Público: o cadastro precisa das versões vigentes antes de existir uma sessão. */
@RestController
@RequestMapping("/api/legal")
public class LegalController {

    @GetMapping("/versions")
    public LegalVersionsResponse versions() {
        return new LegalVersionsResponse(
                LegalDocuments.TERMS_VERSION, LegalDocuments.PRIVACY_VERSION);
    }
}
