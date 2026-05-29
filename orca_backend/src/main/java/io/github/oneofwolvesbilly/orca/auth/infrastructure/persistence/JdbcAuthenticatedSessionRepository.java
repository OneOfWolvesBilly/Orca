package io.github.oneofwolvesbilly.orca.auth.infrastructure.persistence;

import io.github.oneofwolvesbilly.orca.auth.application.AuthenticatedSessionRepository;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedSession;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedSessionId;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedUserId;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

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

    @Override
    public Optional<AuthenticatedUserId> findAuthenticatedUserIdBySessionId(AuthenticatedSessionId sessionId, Instant now) {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(now, "now");

        return jdbcTemplate.query(
                """
                SELECT user_id
                FROM auth_authenticated_sessions
                WHERE session_id = ?
                  AND expires_at > ?
                """,
                (rs, rowNum) -> AuthenticatedUserId.of(rs.getString("user_id")),
                sessionId.value(),
                Timestamp.from(now)
        ).stream().findFirst();
    }
}
