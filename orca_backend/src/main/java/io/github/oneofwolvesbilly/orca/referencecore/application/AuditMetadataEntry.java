package io.github.oneofwolvesbilly.orca.referencecore.application;

import java.util.Objects;

public record AuditMetadataEntry(String key, String value) {

    public AuditMetadataEntry {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        if (key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    public static AuditMetadataEntry of(String key, String value) {
        return new AuditMetadataEntry(key, value);
    }
}
