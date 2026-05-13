package io.github.oneofwolvesbilly.orca.auth.application;

import io.github.oneofwolvesbilly.orca.auth.domain.ProvisioningVerificationRequest;
import io.github.oneofwolvesbilly.orca.auth.domain.ProvisioningVerificationRequestId;

import java.util.Optional;

public interface ProvisioningVerificationRequestRepository {

    Optional<ProvisioningVerificationRequest> findById(ProvisioningVerificationRequestId id);

    void save(ProvisioningVerificationRequest request);
}
