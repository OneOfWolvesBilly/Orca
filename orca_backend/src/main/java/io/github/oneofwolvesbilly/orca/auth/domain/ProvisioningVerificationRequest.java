package io.github.oneofwolvesbilly.orca.auth.domain;

import java.time.Instant;
import java.util.Objects;

/** Auth-owned lifecycle state for confirming provisioning identity verification. */
public final class ProvisioningVerificationRequest {

    private final ProvisioningVerificationRequestId id;
    private final VerificationCode verificationCode;
    private final Instant expiresAt;
    private boolean verified;

    private ProvisioningVerificationRequest(
            ProvisioningVerificationRequestId id,
            VerificationCode verificationCode,
            Instant expiresAt,
            boolean verified
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.verificationCode = Objects.requireNonNull(verificationCode, "verificationCode");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        this.verified = verified;
    }

    public static ProvisioningVerificationRequest pending(
            ProvisioningVerificationRequestId id,
            VerificationCode verificationCode,
            Instant expiresAt
    ) {
        return new ProvisioningVerificationRequest(id, verificationCode, expiresAt, false);
    }

    public static ProvisioningVerificationRequest verified(
            ProvisioningVerificationRequestId id,
            VerificationCode verificationCode,
            Instant expiresAt
    ) {
        return new ProvisioningVerificationRequest(id, verificationCode, expiresAt, true);
    }

    public void confirm(VerificationCode submittedCode, Instant now) {
        Objects.requireNonNull(submittedCode, "submittedCode");
        Objects.requireNonNull(now, "now");

        if (verified || !now.isBefore(expiresAt) || !verificationCode.equals(submittedCode)) {
            throw new ProvisioningIdentityVerificationRejectedException();
        }

        verified = true;
    }

    public ProvisioningVerificationRequestId id() {
        return id;
    }

    public VerificationCode verificationCode() {
        return verificationCode;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public boolean isVerified() {
        return verified;
    }
}
