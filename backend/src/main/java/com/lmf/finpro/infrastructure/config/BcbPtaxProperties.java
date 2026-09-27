package com.lmf.finpro.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "finpro.bcb-ptax")
public record BcbPtaxProperties(String baseUrl, long timeoutMs) {}
