package io.github.oneofwolvesbilly.orca.auth.application;

import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedUserId;
import io.github.oneofwolvesbilly.orca.auth.domain.CurrentUserContext;

import java.util.Objects;

/** Establishes a current authenticated user context for one protected operation. */
public final class EstablishCurrentUserContextUseCase {

    public CurrentUserContext handle(EstablishCurrentUserContextCommand command) {
        Objects.requireNonNull(command, "command");

        int presentedCount = command.presentedAuthenticatedUserIds().size();
        if (presentedCount == 0) {
            throw new UnauthenticatedOperationException();
        }
        if (presentedCount > 1) {
            throw new AmbiguousAuthenticatedUserException();
        }

        return CurrentUserContext.establish(
                AuthenticatedUserId.of(command.presentedAuthenticatedUserIds().get(0))
        );
    }
}
