package io.github.oneofwolvesbilly.orca.auth.web;

import io.github.oneofwolvesbilly.orca.auth.application.ResolveCurrentUserContextFromSessionCommand;
import io.github.oneofwolvesbilly.orca.auth.application.ResolveCurrentUserContextFromSessionUseCase;
import io.github.oneofwolvesbilly.orca.auth.application.UnauthenticatedOperationException;
import io.github.oneofwolvesbilly.orca.auth.domain.CurrentUserContext;

import java.util.Objects;

/** Resolves current user context from the server-side session presented for one HTTP request. */
public final class CurrentUserContextResolver {

    private final ResolveCurrentUserContextFromSessionUseCase resolveCurrentUserContextFromSessionUseCase;

    public CurrentUserContextResolver(ResolveCurrentUserContextFromSessionUseCase resolveCurrentUserContextFromSessionUseCase) {
        this.resolveCurrentUserContextFromSessionUseCase =
                Objects.requireNonNull(resolveCurrentUserContextFromSessionUseCase, "resolveCurrentUserContextFromSessionUseCase");
    }

    public CurrentUserContext resolve(String sessionId) {
        try {
            return resolveCurrentUserContextFromSessionUseCase.handle(
                    new ResolveCurrentUserContextFromSessionCommand(sessionId)
            );
        } catch (UnauthenticatedOperationException | IllegalArgumentException ex) {
            throw new UnauthenticatedHttpRequestException();
        }
    }
}
