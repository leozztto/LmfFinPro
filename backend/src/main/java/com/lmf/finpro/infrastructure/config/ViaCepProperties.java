package com.lmf.finpro.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "finpro.viacep")
public record ViaCepProperties(String baseUrl, long timeoutMs) {}
