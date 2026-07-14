package io.github.oneofwolvesbilly.orca.auth.domain;

import java.time.Instant;
import java.util.Objects;

/** Auth-owned server-side state created after successful password login. */
public record AuthenticatedSession(
        AuthenticatedSessionId id,
        AuthenticatedUserId authenticatedUserId,
        Instant createdAt,
        Instant expiresAt,
        Instant revokedAt
) {

    public AuthenticatedSession {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(authenticatedUserId, "authenticatedUserId");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("session expiresAt must be after createdAt");
        }
        if (revokedAt != null && revokedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("session revokedAt must not be before createdAt");
        }
    }

    public static AuthenticatedSession create(
            AuthenticatedSessionId id,
            AuthenticatedUserId authenticatedUserId,
            Instant createdAt,
            Instant expiresAt
    ) {
        return new AuthenticatedSession(id, authenticatedUserId, createdAt, expiresAt, null);
    }

    public AuthenticatedSession revoke(Instant revokedAt) {
        Objects.requireNonNull(revokedAt, "revokedAt");
        if (this.revokedAt != null) {
            throw new IllegalStateException("session is already revoked");
        }
        if (!expiresAt.isAfter(revokedAt)) {
            throw new IllegalStateException("expired session cannot be revoked");
        }
        return new AuthenticatedSession(id, authenticatedUserId, createdAt, expiresAt, revokedAt);
    }

    public AuthenticatedUserId authenticatedUserIdForSessionUse(Instant now) {
        Objects.requireNonNull(now, "now");
        if (revokedAt != null) {
            throw new IllegalStateException("revoked session cannot be used");
        }
        if (!expiresAt.isAfter(now)) {
            throw new IllegalStateException("expired session cannot be used");
        }
        return authenticatedUserId;
    }
}
