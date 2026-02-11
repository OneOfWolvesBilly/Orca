package io.github.oneofwolvesbilly.orca.organization.domain;

import java.util.Objects;

/** Represents a non-empty group name. */
public record GroupName(String value) {

    public GroupName {
        Objects.requireNonNull(value, "groupName");
        if (value.isBlank()) throw new IllegalArgumentException("group name required");
    }

    /** Creates a group name. */
    public static GroupName of(String value) {
        return new GroupName(value);
    }
}