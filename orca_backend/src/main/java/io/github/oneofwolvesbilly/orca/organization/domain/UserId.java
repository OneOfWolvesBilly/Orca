package io.github.oneofwolvesbilly.orca.organization.domain;

import java.util.Objects;

/** Identifies a user. */
public record UserId(String value) {

    public UserId {
        Objects.requireNonNull(value, "userId");
        if (value.isBlank()) throw new IllegalArgumentException("userId must not be blank");
    }

    /** Creates a user id. */
    public static UserId of(String value) {
        return new UserId(value);
    }
}