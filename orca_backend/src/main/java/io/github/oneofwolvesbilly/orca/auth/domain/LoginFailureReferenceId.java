package io.github.oneofwolvesbilly.orca.auth.domain;

import java.util.Objects;

/** Opaque server-issued reference for one rejected password login attempt. */
public record LoginFailureReferenceId(String value) {

    public LoginFailureReferenceId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("loginFailureReferenceId must not be blank");
        }
    }

    public static LoginFailureReferenceId of(String value) {
        return new LoginFailureReferenceId(value);
    }
}
