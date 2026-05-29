package io.github.oneofwolvesbilly.orca.auth.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginIdentifierTest {

    @Test
    void of_accepts_non_blank_login_identifier() {
        LoginIdentifier identifier = LoginIdentifier.of("employee-login-001");

        assertEquals("employee-login-001", identifier.value());
    }

    @Test
    void of_rejects_blank_login_identifier() {
        assertThrows(IllegalArgumentException.class, () -> LoginIdentifier.of("   "));
    }
}
