package io.github.oneofwolvesbilly.orca.auth.application;

import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedSession;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedSessionId;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedUserId;
import io.github.oneofwolvesbilly.orca.auth.support.FakeAuthenticatedSessionRepository;
import io.github.oneofwolvesbilly.orca.auth.support.FakeRegisteredUserIdentityRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResolveCurrentUserContextFromSessionUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-05-29T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final AuthenticatedSessionId SESSION_ID =
            AuthenticatedSessionId.of("3f1eb30a-86d0-4a3e-89c8-a6ff395ec144");

    private final FakeRegisteredUserIdentityRepository registeredUsers =
            new FakeRegisteredUserIdentityRepository().register("user-1");

    @Test
    void valid_session_establishes_current_user_context() {
        var sessions = new FakeAuthenticatedSessionRepository();
        sessions.save(AuthenticatedSession.create(
                SESSION_ID,
                AuthenticatedUserId.of("user-1"),
                NOW.minusSeconds(60),
                NOW.plusSeconds(60)
        ));
        var useCase = useCase(sessions);

        var context = useCase.handle(new ResolveCurrentUserContextFromSessionCommand(SESSION_ID.value()));

        assertEquals("user-1", context.authenticatedUserId().value());
    }

    @Test
    void missing_session_id_is_rejected_as_unauthenticated() {
        var useCase = useCase(new FakeAuthenticatedSessionRepository());

        assertThrows(UnauthenticatedOperationException.class, () ->
                useCase.handle(new ResolveCurrentUserContextFromSessionCommand(null))
        );
    }

    @Test
    void blank_session_id_is_rejected_as_unauthenticated() {
        var useCase = useCase(new FakeAuthenticatedSessionRepository());

        assertThrows(UnauthenticatedOperationException.class, () ->
                useCase.handle(new ResolveCurrentUserContextFromSessionCommand("   "))
        );
    }

    @Test
    void unknown_session_is_rejected_as_unauthenticated() {
        var useCase = useCase(new FakeAuthenticatedSessionRepository());

        assertThrows(UnauthenticatedOperationException.class, () ->
                useCase.handle(new ResolveCurrentUserContextFromSessionCommand("missing-session"))
        );
    }

    @Test
    void expired_session_is_rejected_as_unauthenticated() {
        var sessions = new FakeAuthenticatedSessionRepository();
        sessions.save(AuthenticatedSession.create(
                SESSION_ID,
                AuthenticatedUserId.of("user-1"),
                NOW.minusSeconds(120),
                NOW.minusSeconds(60)
        ));
        var useCase = useCase(sessions);

        assertThrows(UnauthenticatedOperationException.class, () ->
                useCase.handle(new ResolveCurrentUserContextFromSessionCommand(SESSION_ID.value()))
        );
    }

    @Test
    void session_for_unregistered_user_is_rejected_as_unauthenticated() {
        var sessions = new FakeAuthenticatedSessionRepository();
        sessions.save(AuthenticatedSession.create(
                SESSION_ID,
                AuthenticatedUserId.of("missing-user"),
                NOW.minusSeconds(60),
                NOW.plusSeconds(60)
        ));
        var useCase = useCase(sessions);

        assertThrows(UnauthenticatedOperationException.class, () ->
                useCase.handle(new ResolveCurrentUserContextFromSessionCommand(SESSION_ID.value()))
        );
    }

    private ResolveCurrentUserContextFromSessionUseCase useCase(FakeAuthenticatedSessionRepository sessions) {
        return new ResolveCurrentUserContextFromSessionUseCase(
                sessions,
                new EstablishCurrentUserContextUseCase(registeredUsers),
                CLOCK
        );
    }
}
