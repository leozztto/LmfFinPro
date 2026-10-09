package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.model.OnboardingProgress;
import com.lmf.finpro.domain.model.OnboardingProgress.Step;
import com.lmf.finpro.domain.model.OnboardingProgress.StepId;
import com.lmf.finpro.domain.port.out.OnboardingStatePort;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnboardingStateJdbcAdapter implements OnboardingStatePort {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public OnboardingProgress progress(Long userId) {
        MapSqlParameterSource params = new MapSqlParameterSource("user", userId);
        Set<String> done =
                new HashSet<>(
                        jdbc.queryForList(
                                "SELECT step FROM onboarding_steps_done WHERE user_id = :user",
                                params,
                                String.class));
        List<Step> steps =
                Arrays.stream(StepId.values())
                        .map(step -> new Step(step, done.contains(step.name())))
                        .toList();
        return jdbc.queryForObject(
                """
                SELECT
                  (SELECT dismissed_at IS NOT NULL FROM onboarding_state WHERE user_id = :user)
                      AS dismissed,
                  (SELECT activation_emails_enabled FROM onboarding_state WHERE user_id = :user)
                      AS emails_enabled
                """,
                params,
                (rs, rowNum) ->
                        new OnboardingProgress(
                                steps,
                                rs.getBoolean("dismissed"),
                                // Sem linha gravada vale o padrão: aceita os e-mails.
                                rs.getObject("emails_enabled") == null
                                        || rs.getBoolean("emails_enabled")));
    }

    @Override
    public void recordStep(Long userId, StepId step) {
        jdbc.update(
                "INSERT INTO onboarding_steps_done (user_id, step) VALUES (:user, :step) ON"
                        + " CONFLICT (user_id, step) DO NOTHING",
                new MapSqlParameterSource().addValue("user", userId).addValue("step", step.name()));
    }

    @Override
    public void dismiss(Long userId) {
        jdbc.update(
                "INSERT INTO onboarding_state (user_id, dismissed_at) VALUES (:user, now()) ON"
                        + " CONFLICT (user_id) DO UPDATE SET dismissed_at = COALESCE("
                        + "onboarding_state.dismissed_at, now())",
                new MapSqlParameterSource("user", userId));
    }

    @Override
    public void setActivationEmailsEnabled(Long userId, boolean enabled) {
        jdbc.update(
                "INSERT INTO onboarding_state (user_id, activation_emails_enabled) VALUES (:user,"
                        + " :enabled) ON CONFLICT (user_id) DO UPDATE SET"
                        + " activation_emails_enabled = :enabled",
                new MapSqlParameterSource().addValue("user", userId).addValue("enabled", enabled));
    }
}
