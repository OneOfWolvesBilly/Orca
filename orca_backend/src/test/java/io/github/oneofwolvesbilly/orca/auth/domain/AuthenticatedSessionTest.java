package io.github.oneofwolvesbilly.orca.auth.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticatedSessionTest {

    @Test
    void create_binds_opaque_session_id_to_one_authenticated_user_with_bounded_lifetime() {
        AuthenticatedSessionId sessionId = AuthenticatedSessionId.of("3f1eb30a-86d0-4a3e-89c8-a6ff395ec144");
        AuthenticatedUserId userId = AuthenticatedUserId.of("user-1");
        Instant createdAt = Instant.parse("2026-05-29T00:00:00Z");
        Instant expiresAt = Instant.parse("2026-05-29T08:00:00Z");

        AuthenticatedSession session = AuthenticatedSession.create(sessionId, userId, createdAt, expiresAt);

        assertEquals(sessionId, session.id());
        assertEquals(userId, session.authenticatedUserId());
        assertEquals(createdAt, session.createdAt());
        assertEquals(expiresAt, session.expiresAt());
    }

    @Test
    void create_rejects_unbounded_or_expired_lifetime() {
        AuthenticatedSessionId sessionId = AuthenticatedSessionId.of("3f1eb30a-86d0-4a3e-89c8-a6ff395ec144");
        AuthenticatedUserId userId = AuthenticatedUserId.of("user-1");
        Instant now = Instant.parse("2026-05-29T00:00:00Z");

        assertThrows(IllegalArgumentException.class, () ->
                AuthenticatedSession.create(sessionId, userId, now, now)
        );
    }

    @Test
    void active_session_can_be_revoked_without_changing_identity_or_lifetime() {
        AuthenticatedSessionId sessionId = AuthenticatedSessionId.of("3f1eb30a-86d0-4a3e-89c8-a6ff395ec144");
        AuthenticatedUserId userId = AuthenticatedUserId.of("user-1");
        Instant createdAt = Instant.parse("2026-05-29T00:00:00Z");
        Instant expiresAt = Instant.parse("2026-05-29T08:00:00Z");
        Instant revokedAt = Instant.parse("2026-05-29T01:00:00Z");
        AuthenticatedSession session = AuthenticatedSession.create(sessionId, userId, createdAt, expiresAt);

        AuthenticatedSession revoked = session.revoke(revokedAt);

        assertEquals(sessionId, revoked.id());
        assertEquals(userId, revoked.authenticatedUserId());
        assertEquals(createdAt, revoked.createdAt());
        assertEquals(expiresAt, revoked.expiresAt());
        assertEquals(revokedAt, revoked.revokedAt());
    }

    @Test
    void revoked_session_cannot_establish_current_user_context() {
        AuthenticatedSession session = AuthenticatedSession.create(
                AuthenticatedSessionId.of("3f1eb30a-86d0-4a3e-89c8-a6ff395ec144"),
                AuthenticatedUserId.of("user-1"),
                Instant.parse("2026-05-29T00:00:00Z"),
                Instant.parse("2026-05-29T08:00:00Z")
        );

        AuthenticatedSession revoked = session.revoke(Instant.parse("2026-05-29T01:00:00Z"));

        assertThrows(IllegalStateException.class, () ->
                revoked.authenticatedUserIdForSessionUse(Instant.parse("2026-05-29T02:00:00Z"))
        );
    }

    @Test
    void expired_session_cannot_establish_current_user_context() {
        AuthenticatedSession session = AuthenticatedSession.create(
                AuthenticatedSessionId.of("3f1eb30a-86d0-4a3e-89c8-a6ff395ec144"),
                AuthenticatedUserId.of("user-1"),
                Instant.parse("2026-05-29T00:00:00Z"),
                Instant.parse("2026-05-29T08:00:00Z")
        );

        assertThrows(IllegalStateException.class, () ->
                session.authenticatedUserIdForSessionUse(Instant.parse("2026-05-29T08:00:00Z"))
        );
    }

    @Test
    void revoked_session_cannot_be_revoked_back_to_active() {
        AuthenticatedSession session = AuthenticatedSession.create(
                AuthenticatedSessionId.of("3f1eb30a-86d0-4a3e-89c8-a6ff395ec144"),
                AuthenticatedUserId.of("user-1"),
                Instant.parse("2026-05-29T00:00:00Z"),
                Instant.parse("2026-05-29T08:00:00Z")
        );
        AuthenticatedSession revoked = session.revoke(Instant.parse("2026-05-29T01:00:00Z"));

        assertThrows(IllegalStateException.class, () ->
                revoked.revoke(Instant.parse("2026-05-29T02:00:00Z"))
        );
    }
}
