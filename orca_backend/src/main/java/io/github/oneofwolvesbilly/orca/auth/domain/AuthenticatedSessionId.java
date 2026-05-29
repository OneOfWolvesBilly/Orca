package io.github.oneofwolvesbilly.orca.auth.domain;

import java.util.Objects;

/** Opaque server-issued identifier for server-side authenticated session state. */
public record AuthenticatedSessionId(String value) {

    public AuthenticatedSessionId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
    }

    public static AuthenticatedSessionId of(String value) {
        return new AuthenticatedSessionId(value);
    }
}
