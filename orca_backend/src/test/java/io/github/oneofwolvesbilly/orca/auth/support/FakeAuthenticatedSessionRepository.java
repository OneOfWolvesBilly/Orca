package io.github.oneofwolvesbilly.orca.auth.support;

import io.github.oneofwolvesbilly.orca.auth.application.AuthenticatedSessionRepository;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedSession;

public final class FakeAuthenticatedSessionRepository implements AuthenticatedSessionRepository {

    private AuthenticatedSession savedSession;

    @Override
    public void save(AuthenticatedSession session) {
        savedSession = session;
    }

    public AuthenticatedSession savedSession() {
        return savedSession;
    }
}
