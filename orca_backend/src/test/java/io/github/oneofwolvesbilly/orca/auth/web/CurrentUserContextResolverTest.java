package io.github.oneofwolvesbilly.orca.auth.web;

import io.github.oneofwolvesbilly.orca.auth.application.EstablishCurrentUserContextUseCase;
import io.github.oneofwolvesbilly.orca.auth.application.ResolveCurrentUserContextFromSessionUseCase;
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

class CurrentUserContextResolverTest {

    private static final Instant NOW = Instant.parse("2026-05-29T00:00:00Z");
    private static final AuthenticatedSessionId SESSION_ID =
            AuthenticatedSessionId.of("3f1eb30a-86d0-4a3e-89c8-a6ff395ec144");
    private static final FakeRegisteredUserIdentityRepository repository =
            new FakeRegisteredUserIdentityRepository().register("user-1");
    private static final FakeAuthenticatedSessionRepository sessions = new FakeAuthenticatedSessionRepository();

    static {
        sessions.create(AuthenticatedSession.create(
                SESSION_ID,
                AuthenticatedUserId.of("user-1"),
                NOW.minusSeconds(60),
                NOW.plusSeconds(60)
        ));
    }

    private final CurrentUserContextResolver resolver =
            new CurrentUserContextResolver(new ResolveCurrentUserContextFromSessionUseCase(
                    sessions,
                    new EstablishCurrentUserContextUseCase(repository),
                    Clock.fixed(NOW, ZoneOffset.UTC)
            ));

    @Test
    void resolve_establishes_context_when_valid_session_id_is_presented() {
        var context = resolver.resolve(SESSION_ID.value());

        assertEquals("user-1", context.authenticatedUserId().value());
    }

    @Test
    void resolve_rejects_when_no_session_id_is_presented() {
        assertThrows(UnauthenticatedHttpRequestException.class, () ->
                resolver.resolve(null)
        );
    }

    @Test
    void resolve_rejects_when_blank_session_id_is_presented() {
        assertThrows(UnauthenticatedHttpRequestException.class, () ->
                resolver.resolve(" ")
        );
    }

    @Test
    void resolve_rejects_when_session_is_unknown() {
        assertThrows(UnauthenticatedHttpRequestException.class, () ->
                resolver.resolve("missing-session")
        );
    }

    @Test
    void resolve_rejects_when_session_user_is_not_registered() {
        var unregisteredSessions = new FakeAuthenticatedSessionRepository();
        unregisteredSessions.create(AuthenticatedSession.create(
                AuthenticatedSessionId.of("9f1eb30a-86d0-4a3e-89c8-a6ff395ec144"),
                AuthenticatedUserId.of("missing-user"),
                NOW.minusSeconds(60),
                NOW.plusSeconds(60)
        ));
        var unregisteredResolver = new CurrentUserContextResolver(new ResolveCurrentUserContextFromSessionUseCase(
                unregisteredSessions,
                new EstablishCurrentUserContextUseCase(repository),
                Clock.fixed(NOW, ZoneOffset.UTC)
        ));

        assertThrows(UnauthenticatedHttpRequestException.class, () ->
                unregisteredResolver.resolve("9f1eb30a-86d0-4a3e-89c8-a6ff395ec144")
        );
    }
}
