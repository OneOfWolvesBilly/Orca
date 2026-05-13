package io.github.oneofwolvesbilly.orca.auth.domain;

import java.util.Objects;
import java.util.UUID;

/** Opaque reference for one provisioning identity verification request. */
public record ProvisioningVerificationRequestId(UUID value) {

    public ProvisioningVerificationRequestId {
        Objects.requireNonNull(value, "value");
    }

    public static ProvisioningVerificationRequestId of(String value) {
        Objects.requireNonNull(value, "value");
        return new ProvisioningVerificationRequestId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
