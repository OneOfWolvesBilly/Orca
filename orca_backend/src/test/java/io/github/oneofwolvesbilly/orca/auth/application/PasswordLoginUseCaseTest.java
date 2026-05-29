package io.github.oneofwolvesbilly.orca.auth.application;

import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedSession;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedSessionId;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedUserId;
import io.github.oneofwolvesbilly.orca.auth.domain.LoginIdentifier;
import io.github.oneofwolvesbilly.orca.auth.domain.SubmittedPassword;
import io.github.oneofwolvesbilly.orca.auth.support.FakeAuthenticatedSessionRepository;
import io.github.oneofwolvesbilly.orca.auth.support.FakeLoginCredentialVerifier;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordLoginUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-05-29T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Duration SESSION_LIFETIME = Duration.ofHours(8);
    private static final AuthenticatedSessionId SESSION_ID =
            AuthenticatedSessionId.of("3f1eb30a-86d0-4a3e-89c8-a6ff395ec144");

    @Test
    void handle_creates_and_saves_session_after_successful_credential_verification() {
        var verifier = new FakeLoginCredentialVerifier()
                .accept("employee-login-001", "correct-password", AuthenticatedUserId.of("user-1"));
        var repository = new FakeAuthenticatedSessionRepository();
        var useCase = new PasswordLoginUseCase(verifier, repository, () -> SESSION_ID, CLOCK, SESSION_LIFETIME);

        PasswordLoginResult result = useCase.handle(new PasswordLoginCommand("employee-login-001", "correct-password"));

        AuthenticatedSession saved = repository.savedSession();
        assertEquals(SESSION_ID, result.sessionId());
        assertEquals(NOW.plus(SESSION_LIFETIME), result.expiresAt());
        assertEquals(SESSION_ID, saved.id());
        assertEquals(AuthenticatedUserId.of("user-1"), saved.authenticatedUserId());
        assertEquals(NOW, saved.createdAt());
        assertEquals(NOW.plus(SESSION_LIFETIME), saved.expiresAt());
    }

    @Test
    void handle_rejects_failed_credentials_without_creating_session() {
        var repository = new FakeAuthenticatedSessionRepository();
        var useCase = new PasswordLoginUseCase(
                new FakeLoginCredentialVerifier(),
                repository,
                () -> SESSION_ID,
                CLOCK,
                SESSION_LIFETIME
        );

        assertThrows(LoginRejectedException.class, () ->
                useCase.handle(new PasswordLoginCommand("employee-login-001", "wrong-password"))
        );

        assertNull(repository.savedSession());
    }

    @Test
    void handle_maps_missing_or_blank_inputs_to_login_rejection() {
        var repository = new FakeAuthenticatedSessionRepository();
        var useCase = new PasswordLoginUseCase(
                new FakeLoginCredentialVerifier(),
                repository,
                () -> SESSION_ID,
                CLOCK,
                SESSION_LIFETIME
        );

        assertThrows(LoginRejectedException.class, () -> useCase.handle(new PasswordLoginCommand(null, "password")));
        assertThrows(LoginRejectedException.class, () -> useCase.handle(new PasswordLoginCommand("   ", "password")));
        assertThrows(LoginRejectedException.class, () -> useCase.handle(new PasswordLoginCommand("login", null)));
        assertThrows(LoginRejectedException.class, () -> useCase.handle(new PasswordLoginCommand("login", "   ")));
    }

    @Test
    void credential_verifier_receives_validated_login_identifier_and_password() {
        var verifier = new CapturingLoginCredentialVerifier();
        var useCase = new PasswordLoginUseCase(
                verifier,
                new FakeAuthenticatedSessionRepository(),
                () -> SESSION_ID,
                CLOCK,
                SESSION_LIFETIME
        );

        useCase.handle(new PasswordLoginCommand("employee-login-001", "correct-password"));

        assertEquals(LoginIdentifier.of("employee-login-001"), verifier.capturedIdentifier);
        assertEquals(SubmittedPassword.of("correct-password"), verifier.capturedPassword);
    }

    private static final class CapturingLoginCredentialVerifier implements LoginCredentialVerifier {

        private LoginIdentifier capturedIdentifier;
        private SubmittedPassword capturedPassword;

        @Override
        public AuthenticatedUserId verify(LoginIdentifier loginIdentifier, SubmittedPassword password) {
            capturedIdentifier = loginIdentifier;
            capturedPassword = password;
            return AuthenticatedUserId.of("user-1");
        }
    }
}
