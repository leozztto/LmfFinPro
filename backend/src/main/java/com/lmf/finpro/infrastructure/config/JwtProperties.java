package com.lmf.finpro.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "finpro.jwt")
public record JwtProperties(String secret, long expirationMs) {}
