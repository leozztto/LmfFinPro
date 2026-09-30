package com.lmf.finpro.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

    @Test
    void rejeitaSegredoAusente() {
        assertThatThrownBy(() -> new JwtProperties(null, 3600000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET não configurado");
        assertThatThrownBy(() -> new JwtProperties("  ", 3600000))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejeitaSegredoPadraoPublico() {
        assertThatThrownBy(() -> new JwtProperties("change-me-to-a-long-random-secret", 3600000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("valor de exemplo");
    }

    @Test
    void rejeitaSegredoComMenosDe32Bytes() {
        assertThatThrownBy(() -> new JwtProperties("curto-demais", 3600000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void aceitaSegredoComPeloMenos32Bytes() {
        var props = new JwtProperties("x".repeat(32), 3600000);
        assertThat(props.secret()).hasSize(32);
    }
}
