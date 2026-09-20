package com.lmf.finpro.infrastructure.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "finpro.cors")
public record CorsProperties(List<String> allowedOrigins) {}
