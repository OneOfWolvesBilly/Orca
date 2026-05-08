package io.github.oneofwolvesbilly.orca.organization.infrastructure.auth;

import io.github.oneofwolvesbilly.orca.auth.application.RegisteredUserIdentityRepository;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedUserId;
import io.github.oneofwolvesbilly.orca.organization.application.RegisteredUserDirectory;
import io.github.oneofwolvesbilly.orca.organization.domain.UserId;

import java.util.Objects;

public final class AuthRegisteredUserDirectoryAdapter implements RegisteredUserDirectory {

    private final RegisteredUserIdentityRepository registeredUserIdentityRepository;

    public AuthRegisteredUserDirectoryAdapter(RegisteredUserIdentityRepository registeredUserIdentityRepository) {
        this.registeredUserIdentityRepository =
                Objects.requireNonNull(registeredUserIdentityRepository, "registeredUserIdentityRepository");
    }

    @Override
    public boolean exists(UserId userId) {
        Objects.requireNonNull(userId, "userId");
        return registeredUserIdentityRepository.exists(AuthenticatedUserId.of(userId.value()));
    }
}
