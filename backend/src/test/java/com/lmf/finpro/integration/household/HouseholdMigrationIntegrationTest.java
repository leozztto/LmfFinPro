package com.lmf.finpro.integration.household;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.infrastructure.web.dto.account.AccountRequest;
import com.lmf.finpro.infrastructure.web.dto.account.AccountResponse;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import com.lmf.finpro.integration.support.TestDataFactory;
import com.lmf.finpro.integration.support.TestUser;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;

/** Garantias do schema de grupos (migration V35), exercitadas direto no banco. */
class HouseholdMigrationIntegrationTest extends AbstractIntegrationTest {

    @Autowired private JdbcTemplate jdbc;

    private Long newUser() {
        return jdbc.queryForObject(
                "INSERT INTO users (name, email, password_hash) VALUES ('Teste', ?, 'x')"
                        + " RETURNING id",
                Long.class,
                "u-" + UUID.randomUUID() + "@finpro.test");
    }

    private Long newHousehold(String type) {
        return jdbc.queryForObject(
                "INSERT INTO households (name, type) VALUES ('Casa', ?) RETURNING id",
                Long.class,
                type);
    }

    private void addMember(Long householdId, Long userId, String role) {
        jdbc.update(
                "INSERT INTO household_members (household_id, user_id, role) VALUES (?, ?, ?)",
                householdId,
                userId,
                role);
    }

    private Long newAccount(Long householdId) {
        return jdbc.queryForObject(
                "INSERT INTO accounts (household_id, name, type) VALUES (?, 'Conta', 'CHECKING')"
                        + " RETURNING id",
                Long.class,
                householdId);
    }

    private int countAccounts(Long accountId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM accounts WHERE id = ?", Integer.class, accountId);
    }

    @Test
    void userCanBelongToSeveralHouseholdsButOnlyOncePerHousehold() {
        Long user = newUser();
        Long personal = newHousehold("PERSONAL");
        Long shared = newHousehold("SHARED");

        addMember(personal, user, "OWNER");
        addMember(shared, user, "MEMBER");

        assertThatThrownBy(() -> addMember(shared, user, "OWNER"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void householdTypeIsRestrictedToPersonalOrShared() {
        assertThatThrownBy(() -> newHousehold("TEAM"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void memberRoleIsRestrictedToOwnerOrMember() {
        Long user = newUser();

        assertThatThrownBy(() -> addMember(newHousehold("SHARED"), user, "ADMIN"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void accountRequiresHousehold() {
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO accounts (name, type) VALUES ('Conta',"
                                                + " 'CHECKING')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingAUserKeepsTheHouseholdData() {
        Long household = newHousehold("SHARED");
        Long user = newUser();
        addMember(household, user, "OWNER");
        Long account = newAccount(household);

        jdbc.update("DELETE FROM users WHERE id = ?", user);

        assertThat(countAccounts(account)).isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM household_members WHERE user_id = ?",
                                Integer.class,
                                user))
                .isZero();
    }

    @Test
    void deletingHouseholdRemovesItsData() {
        Long household = newHousehold("SHARED");
        Long account = newAccount(household);

        jdbc.update("DELETE FROM households WHERE id = ?", household);

        assertThat(countAccounts(account)).isZero();
    }

    @Test
    void tagNameIsUniquePerHousehold() {
        Long household = newHousehold("SHARED");
        Long otherHousehold = newHousehold("SHARED");
        jdbc.update("INSERT INTO tags (household_id, name) VALUES (?, 'viagem')", household);

        // Outro grupo pode usar o mesmo nome...
        jdbc.update("INSERT INTO tags (household_id, name) VALUES (?, 'viagem')", otherHousehold);
        // ...mas no mesmo grupo não.
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO tags (household_id, name) VALUES (?,"
                                                + " 'viagem')",
                                        household))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void globalCategoriesKeepNullHousehold() {
        Integer globals =
                jdbc.queryForObject(
                        "SELECT count(*) FROM categories WHERE household_id IS NULL",
                        Integer.class);

        assertThat(globals).isPositive();
    }

    @Test
    void inviteTokenHashIsUnique() {
        Long household = newHousehold("SHARED");
        Long owner = newUser();
        String sql =
                "INSERT INTO household_invites (household_id, email, token_hash, created_by,"
                        + " expires_at) VALUES (?, 'a@b.c', ?, ?, now() + interval '1 day')";
        jdbc.update(sql, household, "hash-1", owner);

        assertThatThrownBy(() -> jdbc.update(sql, household, "hash-1", owner))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void registeringCreatesAPersonalHouseholdOwnedByTheNewUser() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        var row =
                jdbc.queryForMap(
                        "SELECT m.role, h.type FROM household_members m"
                                + " JOIN households h ON h.id = m.household_id WHERE m.user_id = ?",
                        user.userId());

        assertThat(row.get("role")).isEqualTo("OWNER");
        assertThat(row.get("type")).isEqualTo("PERSONAL");
    }

    @Test
    void accountCreatedViaApiBelongsToThePersonalHousehold() {
        TestUser user = TestDataFactory.registerRandomUser(restTemplate);

        AccountResponse account =
                restTemplate
                        .exchange(
                                "/api/accounts",
                                HttpMethod.POST,
                                new HttpEntity<>(
                                        new AccountRequest(
                                                "Minha conta",
                                                AccountType.CHECKING,
                                                BigDecimal.TEN),
                                        user.authHeaders()),
                                AccountResponse.class)
                        .getBody();

        Long accountHousehold =
                jdbc.queryForObject(
                        "SELECT household_id FROM accounts WHERE id = ?", Long.class, account.id());
        Long personalHousehold =
                jdbc.queryForObject(
                        "SELECT m.household_id FROM household_members m"
                                + " JOIN households h ON h.id = m.household_id"
                                + " WHERE m.user_id = ? AND h.type = 'PERSONAL'",
                        Long.class,
                        user.userId());
        assertThat(accountHousehold).isEqualTo(personalHousehold);
    }
}
