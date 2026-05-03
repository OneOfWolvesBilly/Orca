package io.github.oneofwolvesbilly.orca.auth.domain;

import java.util.Objects;

/** Immutable current authenticated user context for a single operation. */
public record CurrentUserContext(AuthenticatedUserId authenticatedUserId) {

    public CurrentUserContext {
        Objects.requireNonNull(authenticatedUserId, "authenticatedUserId");
    }

    public static CurrentUserContext establish(AuthenticatedUserId authenticatedUserId) {
        return new CurrentUserContext(authenticatedUserId);
    }
}
