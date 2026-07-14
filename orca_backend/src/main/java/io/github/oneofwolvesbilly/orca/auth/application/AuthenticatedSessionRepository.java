package io.github.oneofwolvesbilly.orca.auth.application;

import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedSession;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedSessionId;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedUserId;

import java.time.Instant;
import java.util.Optional;

public interface AuthenticatedSessionRepository {

    void create(AuthenticatedSession session);

    void saveRevocation(AuthenticatedSession session);

    Optional<AuthenticatedSession> findBySessionId(AuthenticatedSessionId sessionId);

    Optional<AuthenticatedUserId> findAuthenticatedUserIdBySessionId(AuthenticatedSessionId sessionId, Instant now);
}
