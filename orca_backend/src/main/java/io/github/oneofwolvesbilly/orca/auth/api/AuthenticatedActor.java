package io.github.oneofwolvesbilly.orca.auth.api;

import java.util.Objects;

/** Product-neutral authenticated actor exposed to embedded consumer commands. */
public record AuthenticatedActor(String actorId) {

    public AuthenticatedActor {
        Objects.requireNonNull(actorId, "actorId");
        if (actorId.isBlank()) {
            throw new IllegalArgumentException("actorId must not be blank");
        }
    }

    public static AuthenticatedActor of(String actorId) {
        return new AuthenticatedActor(actorId);
    }
}
