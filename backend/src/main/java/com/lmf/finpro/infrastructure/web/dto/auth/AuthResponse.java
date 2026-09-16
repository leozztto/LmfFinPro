package com.lmf.finpro.infrastructure.web.dto.auth;

public record AuthResponse(String token, Long userId, String name, String email) {
}
