package com.lmf.finpro.infrastructure.web.dto.privacy;

import jakarta.validation.constraints.NotBlank;

/** A senha confirma que quem está pedindo é a dona ou o dono da conta, não só uma sessão aberta. */
public record DeleteAccountRequest(@NotBlank(message = "senha é obrigatória") String password) {}
