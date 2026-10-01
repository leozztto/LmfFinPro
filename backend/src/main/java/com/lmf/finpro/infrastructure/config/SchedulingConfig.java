package com.lmf.finpro.infrastructure.config;

import java.time.Clock;
import java.time.ZoneId;
import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = SchedulingConfig.LOCK_AT_MOST_FOR)
public class SchedulingConfig {

    public static final String ZONE = "America/Sao_Paulo";

    /** Teto da trava caso a réplica que a detém morra no meio do job. */
    public static final String LOCK_AT_MOST_FOR = "PT30M";

    /**
     * Piso da trava: impede que outra réplica, com relógio ligeiramente adiantado, repita o job
     * logo depois de ele terminar.
     */
    public static final String LOCK_AT_LEAST_FOR = "${finpro.scheduling.lock-at-least-for:PT1M}";

    /**
     * "Hoje" das regras de negócio (ex.: quais ocorrências recorrentes já venceram) no fuso do
     * usuário, e não no fuso do servidor — que em nuvem costuma ser UTC e viraria o dia 3h antes.
     */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of(ZONE));
    }

    /** Usa a hora do banco (e não a da JVM) para evitar divergência de relógio entre réplicas. */
    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime()
                        .build());
    }

    /** Para os jobs que também rodam na subida (fora do proxy do {@code @SchedulerLock}). */
    @Bean
    public LockingTaskExecutor lockingTaskExecutor(LockProvider lockProvider) {
        return new DefaultLockingTaskExecutor(lockProvider);
    }
}
