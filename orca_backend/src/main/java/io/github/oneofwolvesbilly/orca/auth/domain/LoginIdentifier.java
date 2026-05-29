package io.github.oneofwolvesbilly.orca.auth.domain;

import java.util.Objects;

/** Opaque client-submitted identifier used only for password login. */
public record LoginIdentifier(String value) {

    public LoginIdentifier {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("loginIdentifier must not be blank");
        }
    }

    public static LoginIdentifier of(String value) {
        return new LoginIdentifier(value);
    }
}
