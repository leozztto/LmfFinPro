package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.model.ActivationCandidate;
import com.lmf.finpro.domain.model.ActivationEmailKind;
import com.lmf.finpro.domain.port.out.ActivationCandidatePort;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActivationCandidateJdbcAdapter implements ActivationCandidatePort {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public List<ActivationCandidate> findCandidates(LocalDateTime createdSince) {
        return jdbc.query(
                """
                SELECT u.id, u.name, u.email, u.created_at,
                  EXISTS (SELECT 1 FROM household_members hm
                          JOIN accounts a ON a.household_id = hm.household_id
                          JOIN transactions t ON t.account_id = a.id
                          WHERE hm.user_id = u.id) AS has_transactions,
                  COALESCE(os.dismissed_at IS NOT NULL, FALSE) AS guide_dismissed,
                  COALESCE((SELECT string_agg(s.kind, ',') FROM activation_emails_sent s
                            WHERE s.user_id = u.id), '') AS sent_kinds
                FROM users u
                LEFT JOIN onboarding_state os ON os.user_id = u.id
                WHERE u.created_at >= :since
                  AND COALESCE(os.activation_emails_enabled, TRUE)
                ORDER BY u.id
                """,
                new MapSqlParameterSource("since", Timestamp.valueOf(createdSince)),
                (rs, rowNum) -> {
                    Set<ActivationEmailKind> sent = EnumSet.noneOf(ActivationEmailKind.class);
                    for (String kind : rs.getString("sent_kinds").split(",")) {
                        if (!kind.isBlank()) {
                            sent.add(ActivationEmailKind.valueOf(kind));
                        }
                    }
                    return new ActivationCandidate(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getBoolean("has_transactions"),
                            rs.getBoolean("guide_dismissed"),
                            sent);
                });
    }

    @Override
    public void markSent(Long userId, ActivationEmailKind kind) {
        jdbc.update(
                "INSERT INTO activation_emails_sent (user_id, kind) VALUES (:user, :kind) ON"
                        + " CONFLICT (user_id, kind) DO NOTHING",
                new MapSqlParameterSource().addValue("user", userId).addValue("kind", kind.name()));
    }
}
