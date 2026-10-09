package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.port.out.PersonalDataExportPort;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Lê os dados do titular por SQL direto, sem passar pelos modelos de domínio, para o pacote de
 * portabilidade sair completo mesmo quando uma tabela ganha coluna nova. A lista de tabelas abaixo
 * é fechada de propósito: tabela nova com dado do usuário precisa ser acrescentada aqui (há um
 * teste que compara com o esquema do banco).
 */
@Component
@RequiredArgsConstructor
public class PersonalDataExportJdbcAdapter implements PersonalDataExportPort {

    /** Colunas do cadastro que nunca saem: segredo de sessão e caminho interno do arquivo. */
    private static final Set<String> USER_HIDDEN_COLUMNS =
            Set.of("password_hash", "session_version", "photo_key");

    /**
     * Tabelas de dados do grupo, na ordem em que aparecem no JSON. O parâmetro {@code :h} é o id do
     * grupo. Categorias e regras globais do sistema (grupo nulo) não entram: não são do titular.
     */
    static final Map<String, String> HOUSEHOLD_QUERIES = householdQueries();

    private static Map<String, String> householdQueries() {
        Map<String, String> q = new LinkedHashMap<>();
        q.put("accounts", "SELECT * FROM accounts WHERE household_id = :h ORDER BY id");
        q.put(
                "account_valuations",
                "SELECT v.* FROM account_valuations v JOIN accounts a ON a.id = v.account_id"
                        + " WHERE a.household_id = :h ORDER BY v.id");
        q.put("categories", "SELECT * FROM categories WHERE household_id = :h ORDER BY id");
        q.put("category_rules", "SELECT * FROM category_rules WHERE household_id = :h ORDER BY id");
        q.put("clients", "SELECT * FROM clients WHERE household_id = :h ORDER BY id");
        q.put("tags", "SELECT * FROM tags WHERE household_id = :h ORDER BY id");
        q.put(
                "transactions",
                "SELECT t.* FROM transactions t JOIN accounts a ON a.id = t.account_id WHERE"
                        + " a.household_id = :h ORDER BY t.transaction_date, t.id");
        q.put(
                "transaction_tags",
                "SELECT tt.* FROM transaction_tags tt JOIN transactions t ON t.id ="
                        + " tt.transaction_id JOIN accounts a ON a.id = t.account_id WHERE"
                        + " a.household_id = :h ORDER BY tt.transaction_id, tt.tag_id");
        q.put(
                "transaction_attachments",
                "SELECT id, transaction_id, document_type, file_name, content_type, size_bytes,"
                        + " created_at FROM transaction_attachments WHERE household_id = :h"
                        + " ORDER BY id");
        q.put("transfers", "SELECT * FROM transfers WHERE household_id = :h ORDER BY id");
        q.put(
                "recurring_transactions",
                "SELECT * FROM recurring_transactions WHERE household_id = :h ORDER BY id");
        q.put(
                "recurring_transaction_tags",
                "SELECT rt.* FROM recurring_transaction_tags rt JOIN recurring_transactions r ON"
                        + " r.id = rt.recurring_transaction_id WHERE r.household_id = :h ORDER BY"
                        + " rt.recurring_transaction_id, rt.tag_id");
        q.put("budgets", "SELECT * FROM budgets WHERE household_id = :h ORDER BY id");
        q.put(
                "recurring_budgets",
                "SELECT * FROM recurring_budgets WHERE household_id = :h ORDER BY id");
        q.put("savings_goals", "SELECT * FROM savings_goals WHERE household_id = :h ORDER BY id");
        q.put(
                "goal_contributions",
                "SELECT c.* FROM goal_contributions c JOIN savings_goals g ON g.id = c.goal_id"
                        + " WHERE g.household_id = :h ORDER BY c.id");
        q.put("debts", "SELECT * FROM debts WHERE household_id = :h ORDER BY id");
        q.put(
                "debt_balances",
                "SELECT b.* FROM debt_balances b JOIN debts d ON d.id = b.debt_id WHERE"
                        + " d.household_id = :h ORDER BY b.id");
        q.put("tax_estimates", "SELECT * FROM tax_estimates WHERE household_id = :h ORDER BY id");
        q.put("import_batches", "SELECT * FROM import_batches WHERE household_id = :h ORDER BY id");
        q.put("pro_labore_settings", "SELECT * FROM pro_labore_settings WHERE household_id = :h");
        return q;
    }

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Map<String, Object> loadUser(Long userId) {
        List<Map<String, Object>> rows =
                query(
                        "SELECT * FROM users WHERE id = :id",
                        new MapSqlParameterSource("id", userId));
        Map<String, Object> user = new LinkedHashMap<>(rows.isEmpty() ? Map.of() : rows.get(0));
        user.keySet().removeAll(USER_HIDDEN_COLUMNS);
        return user;
    }

    @Override
    public Optional<Map<String, Object>> loadNotificationPreferences(Long userId) {
        return query(
                        "SELECT * FROM notification_preferences WHERE user_id = :id",
                        new MapSqlParameterSource("id", userId))
                .stream()
                .findFirst();
    }

    @Override
    public Map<String, Object> loadOnboarding(Long userId) {
        MapSqlParameterSource params = new MapSqlParameterSource("id", userId);
        Map<String, Object> onboarding = new LinkedHashMap<>();
        onboarding.put(
                "estado",
                query(
                                "SELECT dismissed_at, activation_emails_enabled FROM"
                                        + " onboarding_state WHERE user_id = :id",
                                params)
                        .stream()
                        .findFirst()
                        .orElse(null));
        onboarding.put(
                "tarefasConcluidas",
                query(
                        "SELECT step, completed_at FROM onboarding_steps_done WHERE user_id = :id"
                                + " ORDER BY completed_at",
                        params));
        onboarding.put(
                "emailsDeAtivacaoEnviados",
                query(
                        "SELECT kind, sent_at FROM activation_emails_sent WHERE user_id = :id"
                                + " ORDER BY sent_at",
                        params));
        return onboarding;
    }

    @Override
    public List<Map<String, Object>> loadPushDevices(Long userId) {
        return query(
                "SELECT created_at FROM push_subscriptions WHERE user_id = :id ORDER BY id",
                new MapSqlParameterSource("id", userId));
    }

    @Override
    public List<Map<String, Object>> loadMembers(Long householdId) {
        return query(
                "SELECT u.name, m.role, m.joined_at FROM household_members m JOIN users u ON u.id"
                        + " = m.user_id WHERE m.household_id = :h ORDER BY m.joined_at, m.id",
                new MapSqlParameterSource("h", householdId));
    }

    @Override
    public Map<String, List<Map<String, Object>>> loadHouseholdData(Long householdId) {
        MapSqlParameterSource params = new MapSqlParameterSource("h", householdId);
        Map<String, List<Map<String, Object>>> data = new LinkedHashMap<>();
        HOUSEHOLD_QUERIES.forEach((table, sql) -> data.put(table, query(sql, params)));
        return data;
    }

    /**
     * Datas e horas viram LocalDate/LocalDateTime: o Jackson escreveria java.sql.Timestamp em UTC,
     * deslocando as horas em relação ao que a tela mostra (o banco guarda hora local sem fuso).
     */
    private List<Map<String, Object>> query(String sql, MapSqlParameterSource params) {
        return jdbc.queryForList(sql, params).stream()
                .map(PersonalDataExportJdbcAdapter::normalize)
                .toList();
    }

    private static Map<String, Object> normalize(Map<String, Object> row) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        row.forEach(
                (column, value) ->
                        normalized.put(
                                column,
                                value instanceof java.sql.Timestamp timestamp
                                        ? timestamp.toLocalDateTime()
                                        : value instanceof java.sql.Date date
                                                ? date.toLocalDate()
                                                : value));
        return normalized;
    }

    @Override
    public List<StoredAttachment> loadAttachments(Long householdId) {
        return jdbc.query(
                "SELECT id, file_name, storage_key FROM transaction_attachments WHERE"
                        + " household_id = :h ORDER BY id",
                new MapSqlParameterSource("h", householdId),
                (rs, rowNum) ->
                        new StoredAttachment(
                                rs.getLong("id"),
                                rs.getString("file_name"),
                                rs.getString("storage_key")));
    }
}
