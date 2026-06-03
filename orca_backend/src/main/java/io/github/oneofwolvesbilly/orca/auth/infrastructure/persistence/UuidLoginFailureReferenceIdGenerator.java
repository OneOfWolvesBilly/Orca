package io.github.oneofwolvesbilly.orca.auth.infrastructure.persistence;

import io.github.oneofwolvesbilly.orca.auth.application.LoginFailureReferenceIdGenerator;
import io.github.oneofwolvesbilly.orca.auth.domain.LoginFailureReferenceId;

import java.util.UUID;

public final class UuidLoginFailureReferenceIdGenerator implements LoginFailureReferenceIdGenerator {

    @Override
    public LoginFailureReferenceId generate() {
        return LoginFailureReferenceId.of(UUID.randomUUID().toString());
    }
}
