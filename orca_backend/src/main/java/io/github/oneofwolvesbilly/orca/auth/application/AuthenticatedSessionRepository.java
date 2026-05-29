package io.github.oneofwolvesbilly.orca.auth.application;

import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedSession;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedSessionId;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedUserId;

import java.time.Instant;
import java.util.Optional;

public interface AuthenticatedSessionRepository {

    void save(AuthenticatedSession session);

    Optional<AuthenticatedUserId> findAuthenticatedUserIdBySessionId(AuthenticatedSessionId sessionId, Instant now);
}
