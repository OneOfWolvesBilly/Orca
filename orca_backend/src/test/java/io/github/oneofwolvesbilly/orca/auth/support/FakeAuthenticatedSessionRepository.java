package io.github.oneofwolvesbilly.orca.auth.support;

import io.github.oneofwolvesbilly.orca.auth.application.AuthenticatedSessionRepository;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedSession;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedSessionId;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedUserId;

import java.time.Instant;
import java.util.Optional;

public final class FakeAuthenticatedSessionRepository implements AuthenticatedSessionRepository {

    private AuthenticatedSession savedSession;

    @Override
    public void save(AuthenticatedSession session) {
        savedSession = session;
    }

    public AuthenticatedSession savedSession() {
        return savedSession;
    }

    @Override
    public Optional<AuthenticatedUserId> findAuthenticatedUserIdBySessionId(AuthenticatedSessionId sessionId, Instant now) {
        if (savedSession == null) {
            return Optional.empty();
        }
        if (!savedSession.id().equals(sessionId)) {
            return Optional.empty();
        }
        if (!savedSession.expiresAt().isAfter(now)) {
            return Optional.empty();
        }
        return Optional.of(savedSession.authenticatedUserId());
    }
}
