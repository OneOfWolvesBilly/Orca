package io.github.oneofwolvesbilly.orca.auth.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticatedSessionIdTest {

    @Test
    void of_accepts_non_blank_opaque_session_id() {
        AuthenticatedSessionId sessionId = AuthenticatedSessionId.of("3f1eb30a-86d0-4a3e-89c8-a6ff395ec144");

        assertEquals("3f1eb30a-86d0-4a3e-89c8-a6ff395ec144", sessionId.value());
    }

    @Test
    void of_rejects_blank_session_id() {
        assertThrows(IllegalArgumentException.class, () -> AuthenticatedSessionId.of("   "));
    }
}
