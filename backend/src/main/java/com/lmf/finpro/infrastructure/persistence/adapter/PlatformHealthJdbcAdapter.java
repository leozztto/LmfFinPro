package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.port.out.PlatformHealthPort;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformHealthJdbcAdapter implements PlatformHealthPort {

    private static final int VALIDATION_TIMEOUT_SECONDS = 2;

    private final DataSource dataSource;

    @Override
    public boolean isDatabaseAvailable() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(VALIDATION_TIMEOUT_SECONDS);
        } catch (SQLException ex) {
            log.warn("Banco de dados indisponível na verificação de status: {}", ex.getMessage());
            return false;
        }
    }
}
