package io.github.oneofwolvesbilly.orca.auth.application;

import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedSessionId;

@FunctionalInterface
public interface AuthenticatedSessionIdGenerator {

    AuthenticatedSessionId generate();
}
