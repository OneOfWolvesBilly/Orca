package io.github.oneofwolvesbilly.orca.auth.application;

import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedSession;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedSessionId;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedUserId;
import io.github.oneofwolvesbilly.orca.auth.support.FakeAuthenticatedSessionRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertThrows;

class LogoutSessionUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-05-29T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final AuthenticatedSessionId SESSION_ID =
            AuthenticatedSessionId.of("3f1eb30a-86d0-4a3e-89c8-a6ff395ec144");

    @Test
    void logout_with_active_presented_session_persists_revoked_session_state() {
        var sessions = new FakeAuthenticatedSessionRepository();
        sessions.create(AuthenticatedSession.create(
                SESSION_ID,
                AuthenticatedUserId.of("user-1"),
                NOW.minusSeconds(60),
                NOW.plusSeconds(60)
        ));
        var useCase = new LogoutSessionUseCase(sessions, CLOCK);

        useCase.handle(new LogoutSessionCommand(SESSION_ID.value()));

        assertThrows(UnauthenticatedOperationException.class, () ->
                sessions.findAuthenticatedUserIdBySessionId(SESSION_ID, NOW).orElseThrow(UnauthenticatedOperationException::new)
        );
    }

    @Test
    void logout_with_missing_session_input_returns_safe_logout_outcome() {
        var useCase = new LogoutSessionUseCase(new FakeAuthenticatedSessionRepository(), CLOCK);

        useCase.handle(new LogoutSessionCommand(null));
    }

    @Test
    void logout_with_unknown_session_id_returns_safe_logout_outcome() {
        var useCase = new LogoutSessionUseCase(new FakeAuthenticatedSessionRepository(), CLOCK);

        useCase.handle(new LogoutSessionCommand("missing-session"));
    }

    @Test
    void logout_with_expired_session_id_returns_safe_logout_outcome() {
        var sessions = new FakeAuthenticatedSessionRepository();
        sessions.create(AuthenticatedSession.create(
                SESSION_ID,
                AuthenticatedUserId.of("user-1"),
                NOW.minusSeconds(120),
                NOW.minusSeconds(60)
        ));
        var useCase = new LogoutSessionUseCase(sessions, CLOCK);

        useCase.handle(new LogoutSessionCommand(SESSION_ID.value()));
    }
}
