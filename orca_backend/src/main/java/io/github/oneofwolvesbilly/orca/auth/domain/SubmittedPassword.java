package io.github.oneofwolvesbilly.orca.auth.domain;

import java.util.Objects;

/** Client-submitted password input for one login attempt. */
public record SubmittedPassword(String value) {

    public SubmittedPassword {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("password must not be blank");
        }
    }

    public static SubmittedPassword of(String value) {
        return new SubmittedPassword(value);
    }
}
