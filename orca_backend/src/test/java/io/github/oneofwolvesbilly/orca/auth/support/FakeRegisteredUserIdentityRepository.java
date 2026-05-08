package io.github.oneofwolvesbilly.orca.auth.support;

import io.github.oneofwolvesbilly.orca.auth.application.RegisteredUserIdentityRepository;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedUserId;
import io.github.oneofwolvesbilly.orca.auth.domain.RegisteredUserIdentity;

import java.util.HashSet;
import java.util.Set;

public final class FakeRegisteredUserIdentityRepository implements RegisteredUserIdentityRepository {

    private final Set<AuthenticatedUserId> identities = new HashSet<>();

    public FakeRegisteredUserIdentityRepository register(String authenticatedUserId) {
        save(RegisteredUserIdentity.of(AuthenticatedUserId.of(authenticatedUserId)));
        return this;
    }

    @Override
    public void save(RegisteredUserIdentity identity) {
        identities.add(identity.authenticatedUserId());
    }

    @Override
    public boolean exists(AuthenticatedUserId authenticatedUserId) {
        return identities.contains(authenticatedUserId);
    }
}
