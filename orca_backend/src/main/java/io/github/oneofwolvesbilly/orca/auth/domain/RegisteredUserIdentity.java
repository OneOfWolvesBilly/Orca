package io.github.oneofwolvesbilly.orca.auth.domain;

import java.util.Objects;

/** Auth-owned identity for a user recognized by the system. */
public record RegisteredUserIdentity(AuthenticatedUserId authenticatedUserId) {

    public RegisteredUserIdentity {
        Objects.requireNonNull(authenticatedUserId, "authenticatedUserId");
    }

    public static RegisteredUserIdentity of(AuthenticatedUserId authenticatedUserId) {
        return new RegisteredUserIdentity(authenticatedUserId);
    }
}
