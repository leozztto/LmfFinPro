package com.lmf.finpro.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "finpro.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
