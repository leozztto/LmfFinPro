package com.lmf.finpro.infrastructure.config;

import com.lmf.finpro.application.auth.RefreshTokenSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RefreshTokenConfig {

    @Bean
    RefreshTokenSettings refreshTokenSettings(RefreshTokenProperties properties) {
        return new RefreshTokenSettings(properties.ttlDays(), properties.reuseLeewaySeconds());
    }
}
