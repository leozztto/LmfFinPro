package com.lmf.finpro.integration.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base para testes de integração ponta a ponta: sobe o contexto Spring completo (filtros de
 * segurança inclusos) contra um Postgres descartável via Testcontainers.
 *
 * <p>O container é um "singleton" iniciado manualmente (não via @Testcontainers/@Container), de
 * propósito: várias classes de teste estendem esta base, e um campo estático anotado com @Container
 * seria parado pelo JUnit ao final da primeira classe, deixando as classes seguintes com uma
 * conexão morta. Iniciando na inicialização estática e nunca parando explicitamente, o container
 * vive por toda a JVM de teste e é limpo pelo Ryuk.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(AbstractIntegrationTest.JdkHttpClientConfig.class)
public abstract class AbstractIntegrationTest {

    /**
     * O RestTemplateBuilder padrão do TestRestTemplate usa HttpURLConnection, cujo modo de
     * streaming (Content-Length pré-calculado num POST) faz o JDK lançar HttpRetryException em vez
     * de simplesmente devolver 401 quando o servidor exige autenticação. Trocar para o cliente HTTP
     * baseado em java.net.http.HttpClient evita essa armadilha do JDK.
     */
    @TestConfiguration
    static class JdkHttpClientConfig {
        @Bean
        RestTemplateBuilder restTemplateBuilder() {
            return new RestTemplateBuilder()
                    .requestFactory(() -> new JdkClientHttpRequestFactory());
        }
    }

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired protected TestRestTemplate restTemplate;
}
