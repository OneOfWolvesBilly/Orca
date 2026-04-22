package io.github.oneofwolvesbilly.orca.organization.infrastructure.inmemory;

import io.github.oneofwolvesbilly.orca.organization.application.RegisteredUserDirectory;
import io.github.oneofwolvesbilly.orca.organization.domain.UserId;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** In-memory registry for tests and local runs. */
public final class InMemoryRegisteredUserDirectory implements RegisteredUserDirectory {

    private final Set<UserId> registered = new HashSet<>();

    public InMemoryRegisteredUserDirectory register(UserId userId) {
        registered.add(Objects.requireNonNull(userId, "userId"));
        return this;
    }

    public void clear() {
        registered.clear();
    }

    @Override
    public boolean exists(UserId userId) {
        return registered.contains(Objects.requireNonNull(userId, "userId"));
    }
}
