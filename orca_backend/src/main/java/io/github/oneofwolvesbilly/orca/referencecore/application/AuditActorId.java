package io.github.oneofwolvesbilly.orca.referencecore.application;

import java.util.Objects;

public record AuditActorId(String value) {

    public AuditActorId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    public static AuditActorId of(String value) {
        return new AuditActorId(value);
    }
}
