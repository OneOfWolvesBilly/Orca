package io.github.oneofwolvesbilly.orca.referencecore.application;

import java.util.Objects;

public record AuditEventType(String value) {

    public AuditEventType {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    public static AuditEventType of(String value) {
        return new AuditEventType(value);
    }
}
