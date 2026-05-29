package io.github.oneofwolvesbilly.orca.auth.infrastructure.persistence;

import io.github.oneofwolvesbilly.orca.auth.application.LoginCredentialVerifier;
import io.github.oneofwolvesbilly.orca.auth.application.LoginRejectedException;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedUserId;
import io.github.oneofwolvesbilly.orca.auth.domain.LoginIdentifier;
import io.github.oneofwolvesbilly.orca.auth.domain.SubmittedPassword;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JdbcLoginCredentialVerifierTest {

    private LoginCredentialVerifier verifier;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DataSource dataSource = newDataSource();
        migrate(dataSource);
        verifier = new JdbcLoginCredentialVerifier(dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update("INSERT INTO auth_registered_users (user_id) VALUES (?)", "user-1");
    }

    @Test
    void verify_returns_registered_user_for_matching_login_identifier_and_password_hash() {
        insertCredential("employee-login-001", "correct-password", "user-1");

        AuthenticatedUserId userId = verifier.verify(
                LoginIdentifier.of("employee-login-001"),
                SubmittedPassword.of("correct-password")
        );

        assertEquals(AuthenticatedUserId.of("user-1"), userId);
    }

    @Test
    void verify_rejects_unknown_identifier_wrong_password_and_unregistered_user_without_distinction() {
        insertCredential("employee-login-001", "correct-password", "user-1");
        jdbcTemplate.update("INSERT INTO auth_login_credentials (login_identifier, password_hash, user_id) VALUES (?, ?, ?)",
                "orphan-login",
                JdbcLoginCredentialVerifier.hashPasswordForStorage("correct-password"),
                "orphan-user"
        );

        assertThrows(LoginRejectedException.class, () ->
                verifier.verify(LoginIdentifier.of("missing-login"), SubmittedPassword.of("correct-password"))
        );
        assertThrows(LoginRejectedException.class, () ->
                verifier.verify(LoginIdentifier.of("employee-login-001"), SubmittedPassword.of("wrong-password"))
        );
        assertThrows(LoginRejectedException.class, () ->
                verifier.verify(LoginIdentifier.of("orphan-login"), SubmittedPassword.of("correct-password"))
        );
    }

    private void insertCredential(String loginIdentifier, String password, String userId) {
        jdbcTemplate.update(
                "INSERT INTO auth_login_credentials (login_identifier, password_hash, user_id) VALUES (?, ?, ?)",
                loginIdentifier,
                JdbcLoginCredentialVerifier.hashPasswordForStorage(password),
                userId
        );
    }

    private static DataSource newDataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:orca_auth_login_credentials;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
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
