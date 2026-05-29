package io.github.oneofwolvesbilly.orca.auth.application;

import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedSession;

public interface AuthenticatedSessionRepository {

    void save(AuthenticatedSession session);
}
