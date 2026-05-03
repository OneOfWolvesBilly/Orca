package io.github.oneofwolvesbilly.orca.auth.domain;

import java.util.Objects;

/** Identifies the authenticated user for one protected operation. */
public record AuthenticatedUserId(String value) {

    public AuthenticatedUserId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("authenticatedUserId must not be blank");
        }
    }

    public static AuthenticatedUserId of(String value) {
        return new AuthenticatedUserId(value);
    }
}
