package io.github.oneofwolvesbilly.orca.auth.application;

import io.github.oneofwolvesbilly.orca.auth.domain.AuthSystemRole;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedUserId;
import io.github.oneofwolvesbilly.orca.auth.domain.RegisteredUserIdentity;

import java.util.Objects;

public final class ProvisionRegisteredUserIdentityUseCase {

    private final RegisteredUserIdentityRepository registeredUserIdentityRepository;
    private final AuthSystemRoleDirectory authSystemRoleDirectory;

    public ProvisionRegisteredUserIdentityUseCase(
            RegisteredUserIdentityRepository registeredUserIdentityRepository,
            AuthSystemRoleDirectory authSystemRoleDirectory
    ) {
        this.registeredUserIdentityRepository =
                Objects.requireNonNull(registeredUserIdentityRepository, "registeredUserIdentityRepository");
        this.authSystemRoleDirectory = Objects.requireNonNull(authSystemRoleDirectory, "authSystemRoleDirectory");
    }

    public void handle(ProvisionRegisteredUserIdentityCommand command) {
        Objects.requireNonNull(command, "command");

        if (!authSystemRoleDirectory.hasRole(command.actorUserId(), AuthSystemRole.IT_ADMIN)) {
            throw new UnauthorizedAuthOperationException();
        }

        AuthenticatedUserId requestedUserId = AuthenticatedUserId.of(command.requestedUserId());
        if (registeredUserIdentityRepository.exists(requestedUserId)) {
            throw new RegisteredUserIdentityAlreadyExistsException();
        }

        registeredUserIdentityRepository.save(RegisteredUserIdentity.of(requestedUserId));
    }
}
