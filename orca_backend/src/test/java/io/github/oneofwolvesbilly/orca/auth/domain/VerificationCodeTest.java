package io.github.oneofwolvesbilly.orca.auth.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VerificationCodeTest {

    @Test
    void of_accepts_non_blank_code() {
        VerificationCode code = VerificationCode.of("123456");

        assertEquals("123456", code.value());
    }

    @Test
    void of_rejects_blank_code() {
        assertThrows(IllegalArgumentException.class, () -> VerificationCode.of(" "));
    }

    @Test
    void of_rejects_null_code() {
        assertThrows(NullPointerException.class, () -> VerificationCode.of(null));
    }
}
