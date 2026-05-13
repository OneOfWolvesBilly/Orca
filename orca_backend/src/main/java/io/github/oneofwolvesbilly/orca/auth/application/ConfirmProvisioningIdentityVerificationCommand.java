package io.github.oneofwolvesbilly.orca.auth.application;

import java.util.Objects;

public record ConfirmProvisioningIdentityVerificationCommand(
        String verificationRequestId,
        String verificationCode
) {

    public ConfirmProvisioningIdentityVerificationCommand {
        Objects.requireNonNull(verificationRequestId, "verificationRequestId");
        Objects.requireNonNull(verificationCode, "verificationCode");
    }
}
