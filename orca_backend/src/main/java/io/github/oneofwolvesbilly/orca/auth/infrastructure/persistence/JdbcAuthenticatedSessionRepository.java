package io.github.oneofwolvesbilly.orca.auth.infrastructure.persistence;

import io.github.oneofwolvesbilly.orca.auth.application.AuthenticatedSessionRepository;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedSession;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.Objects;

public final class JdbcAuthenticatedSessionRepository implements AuthenticatedSessionRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAuthenticatedSessionRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(Objects.requireNonNull(dataSource, "dataSource"));
    }

    @Override
    public void save(AuthenticatedSession session) {
        Objects.requireNonNull(session, "session");
        jdbcTemplate.update(
                """
                INSERT INTO auth_authenticated_sessions (session_id, user_id, created_at, expires_at)
                VALUES (?, ?, ?, ?)
                """,
                session.id().value(),
                session.authenticatedUserId().value(),
                Timestamp.from(session.createdAt()),
                Timestamp.from(session.expiresAt())
        );
    }
}
