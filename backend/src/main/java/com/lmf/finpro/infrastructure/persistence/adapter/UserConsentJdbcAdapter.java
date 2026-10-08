package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.model.LegalDocumentType;
import com.lmf.finpro.domain.model.UserConsent;
import com.lmf.finpro.domain.port.out.UserConsentRepositoryPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserConsentJdbcAdapter implements UserConsentRepositoryPort {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public void save(Long userId, LegalDocumentType documentType, String version) {
        jdbc.update(
                "INSERT INTO user_consents (user_id, document_type, version) VALUES (:user, :type,"
                        + " :version) ON CONFLICT (user_id, document_type, version) DO NOTHING",
                new MapSqlParameterSource()
                        .addValue("user", userId)
                        .addValue("type", documentType.name())
                        .addValue("version", version));
    }

    @Override
    public List<UserConsent> findByUserId(Long userId) {
        return jdbc.query(
                "SELECT document_type, version, accepted_at FROM user_consents WHERE user_id ="
                        + " :user ORDER BY accepted_at, id",
                new MapSqlParameterSource("user", userId),
                (rs, rowNum) ->
                        new UserConsent(
                                LegalDocumentType.valueOf(rs.getString("document_type")),
                                rs.getString("version"),
                                rs.getTimestamp("accepted_at").toLocalDateTime()));
    }
}
