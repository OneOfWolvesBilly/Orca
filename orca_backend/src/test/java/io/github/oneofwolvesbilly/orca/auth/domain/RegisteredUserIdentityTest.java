package io.github.oneofwolvesbilly.orca.auth.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegisteredUserIdentityTest {

    @Test
    void of_accepts_authenticated_user_id() {
        RegisteredUserIdentity identity = RegisteredUserIdentity.of(AuthenticatedUserId.of("user-1"));

        assertEquals("user-1", identity.authenticatedUserId().value());
    }

    @Test
    void of_rejects_null_authenticated_user_id() {
        assertThrows(NullPointerException.class, () -> RegisteredUserIdentity.of(null));
    }
}
