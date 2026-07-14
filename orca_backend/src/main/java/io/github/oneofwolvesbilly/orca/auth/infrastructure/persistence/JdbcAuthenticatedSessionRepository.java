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
    public void create(AuthenticatedSession session) {
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
    public void saveRevocation(AuthenticatedSession session) {
        Objects.requireNonNull(session, "session");
        if (session.revokedAt() == null) {
            throw new IllegalArgumentException("session must be revoked");
        }

        jdbcTemplate.update(
                """
                UPDATE auth_authenticated_sessions
                SET revoked_at = ?
                WHERE session_id = ?
                """,
                Timestamp.from(session.revokedAt()),
                session.id().value()
        );
    }

    @Override
    public Optional<AuthenticatedSession> findBySessionId(AuthenticatedSessionId sessionId) {
        Objects.requireNonNull(sessionId, "sessionId");

        return jdbcTemplate.query(
                """
                SELECT session_id, user_id, created_at, expires_at, revoked_at
                FROM auth_authenticated_sessions
                WHERE session_id = ?
                """,
                (rs, rowNum) -> new AuthenticatedSession(
                        AuthenticatedSessionId.of(rs.getString("session_id")),
                        AuthenticatedUserId.of(rs.getString("user_id")),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("expires_at").toInstant(),
                        timestampToInstant(rs.getTimestamp("revoked_at"))
                ),
                sessionId.value()
        ).stream().findFirst();
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
                  AND revoked_at IS NULL
                """,
                (rs, rowNum) -> AuthenticatedUserId.of(rs.getString("user_id")),
                sessionId.value(),
                Timestamp.from(now)
        ).stream().findFirst();
    }

    private static Instant timestampToInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
