package io.github.oneofwolvesbilly.orca.auth.support;

import io.github.oneofwolvesbilly.orca.auth.application.ProvisioningVerificationRequestRepository;
import io.github.oneofwolvesbilly.orca.auth.domain.ProvisioningVerificationRequest;
import io.github.oneofwolvesbilly.orca.auth.domain.ProvisioningVerificationRequestId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class FakeProvisioningVerificationRequestRepository implements ProvisioningVerificationRequestRepository {

    private final Map<ProvisioningVerificationRequestId, ProvisioningVerificationRequest> requests = new HashMap<>();
    private ProvisioningVerificationRequest savedRequest;

    public FakeProvisioningVerificationRequestRepository store(ProvisioningVerificationRequest request) {
        requests.put(request.id(), request);
        return this;
    }

    @Override
    public Optional<ProvisioningVerificationRequest> findById(ProvisioningVerificationRequestId id) {
        return Optional.ofNullable(requests.get(id));
    }

    @Override
    public void save(ProvisioningVerificationRequest request) {
        savedRequest = request;
        store(request);
    }

    public ProvisioningVerificationRequest savedRequest() {
        return savedRequest;
    }
}
