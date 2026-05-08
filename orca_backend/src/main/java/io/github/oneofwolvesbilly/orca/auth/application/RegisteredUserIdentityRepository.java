package io.github.oneofwolvesbilly.orca.auth.application;

import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedUserId;
import io.github.oneofwolvesbilly.orca.auth.domain.RegisteredUserIdentity;

public interface RegisteredUserIdentityRepository {

    void save(RegisteredUserIdentity identity);

    boolean exists(AuthenticatedUserId authenticatedUserId);
}
