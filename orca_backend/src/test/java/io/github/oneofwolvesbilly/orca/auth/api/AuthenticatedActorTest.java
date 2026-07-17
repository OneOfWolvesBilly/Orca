package io.github.oneofwolvesbilly.orca.auth.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticatedActorTest {

    @Test
    void creates_actor_with_one_non_blank_id() {
        var actor = AuthenticatedActor.of("user-1");

        assertEquals("user-1", actor.actorId());
    }

    @Test
    void rejects_missing_or_blank_actor_id() {
        assertThrows(NullPointerException.class, () -> AuthenticatedActor.of(null));
        assertThrows(IllegalArgumentException.class, () -> AuthenticatedActor.of("   "));
    }
}
