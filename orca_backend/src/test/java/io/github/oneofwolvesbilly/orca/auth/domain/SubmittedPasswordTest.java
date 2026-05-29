package io.github.oneofwolvesbilly.orca.auth.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SubmittedPasswordTest {

    @Test
    void of_accepts_non_blank_password() {
        SubmittedPassword password = SubmittedPassword.of("correct horse battery staple");

        assertEquals("correct horse battery staple", password.value());
    }

    @Test
    void of_rejects_blank_password() {
        assertThrows(IllegalArgumentException.class, () -> SubmittedPassword.of("   "));
    }
}
