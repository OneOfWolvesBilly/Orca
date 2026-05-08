package io.github.oneofwolvesbilly.orca.auth.infrastructure.persistence;

import io.github.oneofwolvesbilly.orca.auth.domain.AuthSystemRole;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedUserId;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcAuthSystemRoleDirectoryTest {

    private JdbcTemplate jdbcTemplate;
    private JdbcAuthSystemRoleDirectory directory;

    @BeforeEach
    void setUp() {
        DataSource dataSource = newDataSource();
        migrate(dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
        directory = new JdbcAuthSystemRoleDirectory(dataSource);
    }

    @Test
    void hasRole_returns_true_when_registered_user_has_system_role() {
        jdbcTemplate.update("INSERT INTO auth_registered_users (user_id) VALUES (?)", "admin");
        jdbcTemplate.update(
                "INSERT INTO auth_system_role_assignments (user_id, role) VALUES (?, ?)",
                "admin",
                "IT_ADMIN"
        );

        assertTrue(directory.hasRole(AuthenticatedUserId.of("admin"), AuthSystemRole.IT_ADMIN));
    }

    @Test
    void hasRole_returns_false_when_registered_user_lacks_system_role() {
        jdbcTemplate.update("INSERT INTO auth_registered_users (user_id) VALUES (?)", "user-1");

        assertFalse(directory.hasRole(AuthenticatedUserId.of("user-1"), AuthSystemRole.IT_ADMIN));
    }

    private static DataSource newDataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:orca_auth_roles;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
        return dataSource;
    }

    private static void migrate(DataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load()
                .clean();

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load()
                .migrate();
    }
}
