package io.github.oneofwolvesbilly.orca.auth.support;

import io.github.oneofwolvesbilly.orca.auth.application.AuthenticatedSessionRepository;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedSession;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedSessionId;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedUserId;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class FakeAuthenticatedSessionRepository implements AuthenticatedSessionRepository {

    private final Map<AuthenticatedSessionId, AuthenticatedSession> sessions = new HashMap<>();

    @Override
    public void create(AuthenticatedSession session) {
        Objects.requireNonNull(session, "session");
        if (sessions.putIfAbsent(session.id(), session) != null) {
            throw new IllegalStateException("session already exists");
        }
    }

    @Override
    public void saveRevocation(AuthenticatedSession session) {
        Objects.requireNonNull(session, "session");
        if (session.revokedAt() == null) {
            throw new IllegalArgumentException("session must be revoked");
        }
        if (sessions.containsKey(session.id())) {
            sessions.put(session.id(), session);
        }
    }

    public AuthenticatedSession savedSession() {
        return sessions.values().stream().findFirst().orElse(null);
    }

    @Override
    public Optional<AuthenticatedSession> findBySessionId(AuthenticatedSessionId sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public Optional<AuthenticatedUserId> findAuthenticatedUserIdBySessionId(AuthenticatedSessionId sessionId, Instant now) {
        return findBySessionId(sessionId)
                .flatMap(session -> authenticatedUserIdForSessionUse(session, now));
    }

    private static Optional<AuthenticatedUserId> authenticatedUserIdForSessionUse(
            AuthenticatedSession session,
            Instant now
    ) {
        try {
            return Optional.of(session.authenticatedUserIdForSessionUse(now));
        } catch (IllegalStateException ex) {
            return Optional.empty();
        }
    }
}
