package io.github.oneofwolvesbilly.orca.auth.application;

import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedUserId;

import java.util.Objects;

public record ProvisionRegisteredUserIdentityCommand(
        AuthenticatedUserId actorUserId,
        String requestedUserId
) {

    public ProvisionRegisteredUserIdentityCommand {
        Objects.requireNonNull(actorUserId, "actorUserId");
        Objects.requireNonNull(requestedUserId, "requestedUserId");
    }
}
