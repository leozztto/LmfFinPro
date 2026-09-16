package com.lmf.finpro.application.auth;

public record LoginCommand(String email, String rawPassword) {
}
