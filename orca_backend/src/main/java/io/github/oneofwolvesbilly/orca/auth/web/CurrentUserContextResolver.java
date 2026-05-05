package io.github.oneofwolvesbilly.orca.auth.web;

import io.github.oneofwolvesbilly.orca.auth.application.AmbiguousAuthenticatedUserException;
import io.github.oneofwolvesbilly.orca.auth.application.EstablishCurrentUserContextCommand;
import io.github.oneofwolvesbilly.orca.auth.application.EstablishCurrentUserContextUseCase;
import io.github.oneofwolvesbilly.orca.auth.application.UnauthenticatedOperationException;
import io.github.oneofwolvesbilly.orca.auth.domain.CurrentUserContext;

import java.util.List;
import java.util.Objects;

/** Resolves current user context from the authenticated identities presented for one HTTP request. */
public final class CurrentUserContextResolver {

    private final EstablishCurrentUserContextUseCase establishCurrentUserContextUseCase;

    public CurrentUserContextResolver(EstablishCurrentUserContextUseCase establishCurrentUserContextUseCase) {
        this.establishCurrentUserContextUseCase =
                Objects.requireNonNull(establishCurrentUserContextUseCase, "establishCurrentUserContextUseCase");
    }

    public CurrentUserContext resolve(List<String> presentedUserIds) {
        Objects.requireNonNull(presentedUserIds, "presentedUserIds");

        try {
            return establishCurrentUserContextUseCase.handle(
                    new EstablishCurrentUserContextCommand(presentedUserIds)
            );
        } catch (UnauthenticatedOperationException
                 | AmbiguousAuthenticatedUserException
                 | IllegalArgumentException ex) {
            throw new UnauthenticatedHttpRequestException();
        }
    }
}
