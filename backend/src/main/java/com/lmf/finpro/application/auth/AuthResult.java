package com.lmf.finpro.application.auth;

public record AuthResult(String token, Long userId, String name, String email) {}
