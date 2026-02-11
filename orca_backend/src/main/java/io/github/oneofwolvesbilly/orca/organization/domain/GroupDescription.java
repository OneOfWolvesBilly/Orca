package io.github.oneofwolvesbilly.orca.organization.domain;

import java.util.Objects;

/** Represents a group description. */
public record GroupDescription(String value) {

    public GroupDescription {
        Objects.requireNonNull(value, "groupDescription");
        // Allow blank unless spec requires stricter rules.
    }

    /** Creates a group description. */
    public static GroupDescription of(String value) {
        return new GroupDescription(value);
    }
}