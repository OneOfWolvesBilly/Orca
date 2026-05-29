package io.github.oneofwolvesbilly.orca.auth.infrastructure.persistence;

import io.github.oneofwolvesbilly.orca.auth.application.AuthenticatedSessionRepository;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedSession;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedSessionId;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedUserId;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JdbcAuthenticatedSessionRepositoryTest {

    private static final String SESSION_ID = "3f1eb30a-86d0-4a3e-89c8-a6ff395ec144";
    private static final Instant CREATED_AT = Instant.parse("2026-05-29T00:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-05-29T08:00:00Z");

    private AuthenticatedSessionRepository repository;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DataSource dataSource = newDataSource();
        migrate(dataSource);
        repository = new JdbcAuthenticatedSessionRepository(dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update("INSERT INTO auth_registered_users (user_id) VALUES (?)", "user-1");
    }

    @Test
    void save_persists_server_side_session_state() {
        repository.save(AuthenticatedSession.create(
                AuthenticatedSessionId.of(SESSION_ID),
                AuthenticatedUserId.of("user-1"),
                CREATED_AT,
                EXPIRES_AT
        ));

        String userId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM auth_authenticated_sessions WHERE session_id = ?",
                String.class,
                SESSION_ID
        );
        Timestamp createdAt = jdbcTemplate.queryForObject(
                "SELECT created_at FROM auth_authenticated_sessions WHERE session_id = ?",
                Timestamp.class,
                SESSION_ID
        );
        Timestamp expiresAt = jdbcTemplate.queryForObject(
                "SELECT expires_at FROM auth_authenticated_sessions WHERE session_id = ?",
                Timestamp.class,
                SESSION_ID
        );

        assertEquals("user-1", userId);
        assertEquals(CREATED_AT, createdAt.toInstant());
        assertEquals(EXPIRES_AT, expiresAt.toInstant());
    }

    private static DataSource newDataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:orca_auth_sessions;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
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
