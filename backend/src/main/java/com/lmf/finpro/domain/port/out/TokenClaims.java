package com.lmf.finpro.domain.port.out;

public record TokenClaims(Long userId, String email, int sessionVersion) {}
