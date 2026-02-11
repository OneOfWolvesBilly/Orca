package io.github.oneofwolvesbilly.orca.organization.domain;

import java.util.Objects;

/** Identifies a group. */
public record GroupId(String value) {

    public GroupId {
        Objects.requireNonNull(value, "groupId");
        if (value.isBlank()) throw new IllegalArgumentException("groupId must not be blank");
    }

    /** Creates a group id. */
    public static GroupId of(String value) {
        return new GroupId(value);
    }
}