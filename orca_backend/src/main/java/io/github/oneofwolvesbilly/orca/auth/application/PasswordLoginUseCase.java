package io.github.oneofwolvesbilly.orca.auth.application;

import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedSession;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedSessionId;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedUserId;
import io.github.oneofwolvesbilly.orca.auth.domain.LoginIdentifier;
import io.github.oneofwolvesbilly.orca.auth.domain.SubmittedPassword;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class PasswordLoginUseCase {

    private final LoginCredentialVerifier credentialVerifier;
    private final AuthenticatedSessionRepository sessionRepository;
    private final AuthenticatedSessionIdGenerator sessionIdGenerator;
    private final Clock clock;
    private final Duration sessionLifetime;

    public PasswordLoginUseCase(
            LoginCredentialVerifier credentialVerifier,
            AuthenticatedSessionRepository sessionRepository,
            AuthenticatedSessionIdGenerator sessionIdGenerator,
            Clock clock,
            Duration sessionLifetime
    ) {
        this.credentialVerifier = Objects.requireNonNull(credentialVerifier, "credentialVerifier");
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "sessionRepository");
        this.sessionIdGenerator = Objects.requireNonNull(sessionIdGenerator, "sessionIdGenerator");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.sessionLifetime = Objects.requireNonNull(sessionLifetime, "sessionLifetime");
    }

    public PasswordLoginResult handle(PasswordLoginCommand command) {
        Objects.requireNonNull(command, "command");

        LoginIdentifier loginIdentifier = parseLoginIdentifier(command.loginIdentifier());
        SubmittedPassword password = parsePassword(command.password());
        AuthenticatedUserId authenticatedUserId = credentialVerifier.verify(loginIdentifier, password);
        if (authenticatedUserId == null) {
            throw new LoginRejectedException();
        }

        Instant createdAt = clock.instant();
        Instant expiresAt = createdAt.plus(sessionLifetime);
        AuthenticatedSessionId sessionId = sessionIdGenerator.generate();
        AuthenticatedSession session = AuthenticatedSession.create(sessionId, authenticatedUserId, createdAt, expiresAt);
        sessionRepository.save(session);
        return new PasswordLoginResult(sessionId, expiresAt);
    }

    private LoginIdentifier parseLoginIdentifier(String value) {
        try {
            return LoginIdentifier.of(value);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new LoginRejectedException();
        }
    }

    private SubmittedPassword parsePassword(String value) {
        try {
            return SubmittedPassword.of(value);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new LoginRejectedException();
        }
    }
}
