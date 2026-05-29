package io.github.oneofwolvesbilly.orca.auth.infrastructure.persistence;

import io.github.oneofwolvesbilly.orca.auth.application.AuthenticatedSessionIdGenerator;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedSessionId;

import java.util.UUID;

public final class UuidAuthenticatedSessionIdGenerator implements AuthenticatedSessionIdGenerator {

    @Override
    public AuthenticatedSessionId generate() {
        return AuthenticatedSessionId.of(UUID.randomUUID().toString());
    }
}
