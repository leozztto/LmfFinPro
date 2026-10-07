package com.lmf.finpro.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Trava de segurança da portabilidade (LGPD): quem criar uma tabela nova precisa decidir se ela
 * entra na exportação dos dados do usuário. Sem isso, um dado novo ficaria de fora do ZIP sem que
 * ninguém percebesse.
 */
class PersonalDataExportCoverageTest extends AbstractIntegrationTest {

    /**
     * Tabelas que não são exportadas como "dados do grupo", cada uma com o motivo:
     *
     * <ul>
     *   <li>users, notification_preferences, push_subscriptions, user_consents: exportadas à parte,
     *       por usuário (sem segredos).
     *   <li>households, household_members: viram o nome, o tipo e a lista de membros do grupo.
     *   <li>household_invites, refresh_tokens, password_reset_tokens, sent_alerts: segredos de
     *       sessão/convite ou controle técnico, sem dado do titular.
     *   <li>account_deletion_log: registro de exclusões, sem dado pessoal.
     *   <li>exchange_rates, shedlock, flyway_schema_history: dados do sistema, não do usuário.
     * </ul>
     */
    private static final Set<String> NOT_EXPORTED_AS_GROUP_DATA =
            Set.of(
                    "users",
                    "notification_preferences",
                    "push_subscriptions",
                    "user_consents",
                    "households",
                    "household_members",
                    "household_invites",
                    "refresh_tokens",
                    "password_reset_tokens",
                    "sent_alerts",
                    "account_deletion_log",
                    "exchange_rates",
                    "shedlock",
                    "flyway_schema_history");

    @Autowired private JdbcTemplate jdbc;

    @Test
    void everyTableIsEitherExportedOrExplicitlyListedAsNotExported() {
        List<String> tables =
                jdbc.queryForList(
                        "SELECT table_name FROM information_schema.tables WHERE table_schema ="
                                + " current_schema() AND table_type = 'BASE TABLE'",
                        String.class);

        Set<String> expected =
                new HashSet<>(PersonalDataExportJdbcAdapter.HOUSEHOLD_QUERIES.keySet());
        expected.addAll(NOT_EXPORTED_AS_GROUP_DATA);

        assertThat(tables)
                .as(
                        "Tabela nova: acrescente a consulta em PersonalDataExportJdbcAdapter ou"
                                + " liste-a aqui com o motivo")
                .containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    void everyExportQueryRunsAgainstTheRealSchema() {
        PersonalDataExportJdbcAdapter adapter =
                new PersonalDataExportJdbcAdapter(
                        new org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate(
                                jdbc));

        // Um grupo que não existe: o que importa é o SQL ser válido em todas as tabelas.
        assertThat(adapter.loadHouseholdData(-1L).keySet())
                .containsExactlyElementsOf(
                        PersonalDataExportJdbcAdapter.HOUSEHOLD_QUERIES.keySet());
    }
}
