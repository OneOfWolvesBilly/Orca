package io.github.oneofwolvesbilly.orca.referencecore.infrastructure.persistence;

import io.github.oneofwolvesbilly.orca.referencecore.application.ClientFailureReferenceId;
import io.github.oneofwolvesbilly.orca.referencecore.application.ClientFailureReferenceIdGenerator;

import java.util.UUID;

public final class UuidClientFailureReferenceIdGenerator implements ClientFailureReferenceIdGenerator {

    @Override
    public ClientFailureReferenceId generate() {
        return ClientFailureReferenceId.of(UUID.randomUUID().toString());
    }
}
