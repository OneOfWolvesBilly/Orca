package io.github.oneofwolvesbilly.orca.auth.application;

import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedSessionId;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedUserId;
import io.github.oneofwolvesbilly.orca.auth.domain.CurrentUserContext;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

public final class ResolveCurrentUserContextFromSessionUseCase {

    private final AuthenticatedSessionRepository authenticatedSessionRepository;
    private final EstablishCurrentUserContextUseCase establishCurrentUserContextUseCase;
    private final Clock clock;

    public ResolveCurrentUserContextFromSessionUseCase(
            AuthenticatedSessionRepository authenticatedSessionRepository,
            EstablishCurrentUserContextUseCase establishCurrentUserContextUseCase,
            Clock clock
    ) {
        this.authenticatedSessionRepository =
                Objects.requireNonNull(authenticatedSessionRepository, "authenticatedSessionRepository");
        this.establishCurrentUserContextUseCase =
                Objects.requireNonNull(establishCurrentUserContextUseCase, "establishCurrentUserContextUseCase");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public CurrentUserContext handle(ResolveCurrentUserContextFromSessionCommand command) {
        Objects.requireNonNull(command, "command");

        AuthenticatedSessionId sessionId = sessionId(command.sessionId());
        AuthenticatedUserId authenticatedUserId = authenticatedSessionRepository
                .findAuthenticatedUserIdBySessionId(sessionId, clock.instant())
                .orElseThrow(UnauthenticatedOperationException::new);

        return establishCurrentUserContextUseCase.handle(
                new EstablishCurrentUserContextCommand(List.of(authenticatedUserId.value()))
        );
    }

    private static AuthenticatedSessionId sessionId(String value) {
        try {
            return AuthenticatedSessionId.of(value);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new UnauthenticatedOperationException();
        }
    }
}
