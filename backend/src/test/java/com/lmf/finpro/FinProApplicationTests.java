package com.lmf.finpro;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
class FinProApplicationTests {

    @Test
    void contextLoads() {
        // Sobe o contexto do Spring como smoke test inicial.
        // Testes de integração com Testcontainers virão em classes específicas.
    }
}
