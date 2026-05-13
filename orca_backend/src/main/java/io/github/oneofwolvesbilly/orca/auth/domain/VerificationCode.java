package io.github.oneofwolvesbilly.orca.auth.domain;

import java.util.Objects;

/** Server-issued code used to confirm a provisioning verification request. */
public record VerificationCode(String value) {

    public VerificationCode {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("verificationCode must not be blank");
        }
    }

    public static VerificationCode of(String value) {
        return new VerificationCode(value);
    }
}
