package io.github.oneofwolvesbilly.orca.referencecore.application;

import java.util.Objects;
import java.util.UUID;

public record ClientFailureReferenceId(String value) {

    public ClientFailureReferenceId {
        Objects.requireNonNull(value, "value");
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Client failure reference id must be a UUID");
        }
    }

    public static ClientFailureReferenceId of(String value) {
        return new ClientFailureReferenceId(value);
    }
}
