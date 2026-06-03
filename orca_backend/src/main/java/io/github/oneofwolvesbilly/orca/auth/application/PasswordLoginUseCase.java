package io.github.oneofwolvesbilly.orca.auth.application;

import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedSession;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedSessionId;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedUserId;
import io.github.oneofwolvesbilly.orca.auth.domain.LoginFailureAuditRecord;
import io.github.oneofwolvesbilly.orca.auth.domain.LoginFailureReason;
import io.github.oneofwolvesbilly.orca.auth.domain.LoginFailureReferenceId;
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
    private final LoginFailureAuditRecordRepository loginFailureAuditRecordRepository;
    private final LoginFailureReferenceIdGenerator loginFailureReferenceIdGenerator;
    private final Clock clock;
    private final Duration sessionLifetime;

    public PasswordLoginUseCase(
            LoginCredentialVerifier credentialVerifier,
            AuthenticatedSessionRepository sessionRepository,
            AuthenticatedSessionIdGenerator sessionIdGenerator,
            LoginFailureAuditRecordRepository loginFailureAuditRecordRepository,
            LoginFailureReferenceIdGenerator loginFailureReferenceIdGenerator,
            Clock clock,
            Duration sessionLifetime
    ) {
        this.credentialVerifier = Objects.requireNonNull(credentialVerifier, "credentialVerifier");
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "sessionRepository");
        this.sessionIdGenerator = Objects.requireNonNull(sessionIdGenerator, "sessionIdGenerator");
        this.loginFailureAuditRecordRepository =
                Objects.requireNonNull(loginFailureAuditRecordRepository, "loginFailureAuditRecordRepository");
        this.loginFailureReferenceIdGenerator =
                Objects.requireNonNull(loginFailureReferenceIdGenerator, "loginFailureReferenceIdGenerator");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.sessionLifetime = Objects.requireNonNull(sessionLifetime, "sessionLifetime");
    }

    public PasswordLoginResult handle(PasswordLoginCommand command) {
        Objects.requireNonNull(command, "command");

        LoginIdentifier loginIdentifier = parseLoginIdentifier(command.loginIdentifier());
        SubmittedPassword password = parsePassword(command.password(), command.loginIdentifier());
        AuthenticatedUserId authenticatedUserId;
        try {
            authenticatedUserId = credentialVerifier.verify(loginIdentifier, password);
        } catch (LoginRejectedException ex) {
            throw recordAndReject(command.loginIdentifier(), LoginFailureReason.INVALID_CREDENTIALS);
        }
        if (authenticatedUserId == null) {
            throw recordAndReject(command.loginIdentifier(), LoginFailureReason.INVALID_CREDENTIALS);
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
            throw recordAndReject(value, LoginFailureReason.INVALID_INPUT);
        }
    }

    private SubmittedPassword parsePassword(String value, String submittedLoginIdentifier) {
        try {
            return SubmittedPassword.of(value);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw recordAndReject(submittedLoginIdentifier, LoginFailureReason.INVALID_INPUT);
        }
    }

    private LoginRejectedException recordAndReject(String submittedLoginIdentifier, LoginFailureReason reason) {
        LoginFailureReferenceId referenceId = loginFailureReferenceIdGenerator.generate();
        LoginFailureAuditRecord record = LoginFailureAuditRecord.create(
                referenceId,
                clock.instant(),
                submittedLoginIdentifier,
                reason
        );
        loginFailureAuditRecordRepository.save(record);
        return new LoginRejectedException(referenceId);
    }
}
