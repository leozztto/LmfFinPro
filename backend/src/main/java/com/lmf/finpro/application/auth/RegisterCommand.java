package com.lmf.finpro.application.auth;

public record RegisterCommand(String name, String email, String rawPassword, String taxRegime) {
}
