package io.github.oneofwolvesbilly.orca.auth.application;

import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedSession;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedSessionId;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class LogoutSessionUseCase {

    private final AuthenticatedSessionRepository authenticatedSessionRepository;
    private final Clock clock;

    public LogoutSessionUseCase(
            AuthenticatedSessionRepository authenticatedSessionRepository,
            Clock clock
    ) {
        this.authenticatedSessionRepository =
                Objects.requireNonNull(authenticatedSessionRepository, "authenticatedSessionRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public void handle(LogoutSessionCommand command) {
        Objects.requireNonNull(command, "command");

        AuthenticatedSessionId sessionId = sessionId(command.sessionId());
        if (sessionId == null) {
            return;
        }

        Instant now = clock.instant();
        authenticatedSessionRepository.findBySessionId(sessionId)
                .flatMap(session -> revoke(session, now))
                .ifPresent(authenticatedSessionRepository::saveRevocation);
    }

    private static AuthenticatedSessionId sessionId(String value) {
        try {
            return AuthenticatedSessionId.of(value);
        } catch (IllegalArgumentException | NullPointerException ex) {
            return null;
        }
    }

    private static java.util.Optional<AuthenticatedSession> revoke(AuthenticatedSession session, Instant now) {
        try {
            return java.util.Optional.of(session.revoke(now));
        } catch (IllegalStateException ex) {
            return java.util.Optional.empty();
        }
    }
}
