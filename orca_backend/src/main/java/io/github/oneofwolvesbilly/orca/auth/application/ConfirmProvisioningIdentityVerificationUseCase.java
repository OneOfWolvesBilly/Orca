package io.github.oneofwolvesbilly.orca.auth.application;

import io.github.oneofwolvesbilly.orca.auth.domain.ProvisioningIdentityVerificationRejectedException;
import io.github.oneofwolvesbilly.orca.auth.domain.ProvisioningVerificationRequest;
import io.github.oneofwolvesbilly.orca.auth.domain.ProvisioningVerificationRequestId;
import io.github.oneofwolvesbilly.orca.auth.domain.VerificationCode;

import java.time.Clock;
import java.util.Objects;

public final class ConfirmProvisioningIdentityVerificationUseCase {

    private final ProvisioningVerificationRequestRepository repository;
    private final Clock clock;

    public ConfirmProvisioningIdentityVerificationUseCase(
            ProvisioningVerificationRequestRepository repository,
            Clock clock
    ) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public void handle(ConfirmProvisioningIdentityVerificationCommand command) {
        Objects.requireNonNull(command, "command");

        ProvisioningVerificationRequestId id = parseId(command.verificationRequestId());
        VerificationCode code = parseCode(command.verificationCode());
        ProvisioningVerificationRequest request = repository.findById(id)
                .orElseThrow(ProvisioningIdentityVerificationRejectedException::new);

        request.confirm(code, clock.instant());
        repository.save(request);
    }

    private ProvisioningVerificationRequestId parseId(String value) {
        try {
            return ProvisioningVerificationRequestId.of(value);
        } catch (IllegalArgumentException ex) {
            throw new ProvisioningIdentityVerificationRejectedException();
        }
    }

    private VerificationCode parseCode(String value) {
        try {
            return VerificationCode.of(value);
        } catch (IllegalArgumentException ex) {
            throw new ProvisioningIdentityVerificationRejectedException();
        }
    }
}
