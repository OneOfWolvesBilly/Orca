package io.github.oneofwolvesbilly.orca.auth.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginFailureReferenceIdTest {

    @Test
    void of_accepts_non_blank_opaque_reference_id() {
        LoginFailureReferenceId referenceId =
                LoginFailureReferenceId.of("7f1eb30a-86d0-4a3e-89c8-a6ff395ec144");

        assertEquals("7f1eb30a-86d0-4a3e-89c8-a6ff395ec144", referenceId.value());
    }

    @Test
    void of_rejects_blank_reference_id() {
        assertThrows(IllegalArgumentException.class, () -> LoginFailureReferenceId.of("   "));
    }
}
