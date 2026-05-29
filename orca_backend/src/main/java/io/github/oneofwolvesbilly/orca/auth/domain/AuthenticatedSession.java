package io.github.oneofwolvesbilly.orca.auth.domain;

import java.time.Instant;
import java.util.Objects;

/** Auth-owned server-side state created after successful password login. */
public record AuthenticatedSession(
        AuthenticatedSessionId id,
        AuthenticatedUserId authenticatedUserId,
        Instant createdAt,
        Instant expiresAt
) {

    public AuthenticatedSession {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(authenticatedUserId, "authenticatedUserId");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("session expiresAt must be after createdAt");
        }
    }

    public static AuthenticatedSession create(
            AuthenticatedSessionId id,
            AuthenticatedUserId authenticatedUserId,
            Instant createdAt,
            Instant expiresAt
    ) {
        return new AuthenticatedSession(id, authenticatedUserId, createdAt, expiresAt);
    }
}
