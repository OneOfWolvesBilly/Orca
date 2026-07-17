package io.github.oneofwolvesbilly.orca.referencecore.application;

import java.util.Objects;

public record AuditOutcome(String value) {

    public AuditOutcome {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    public static AuditOutcome of(String value) {
        return new AuditOutcome(value);
    }
}
