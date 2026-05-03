package io.github.oneofwolvesbilly.orca.auth.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticatedUserIdTest {

    @Test
    void of_accepts_non_blank_user_id() {
        var authenticatedUserId = AuthenticatedUserId.of("user-1");

        assertEquals("user-1", authenticatedUserId.value());
    }

    @Test
    void of_rejects_blank_user_id() {
        assertThrows(IllegalArgumentException.class, () -> AuthenticatedUserId.of(" "));
    }
}
