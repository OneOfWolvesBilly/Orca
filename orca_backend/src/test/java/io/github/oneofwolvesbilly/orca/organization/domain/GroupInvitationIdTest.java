package io.github.oneofwolvesbilly.orca.organization.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class GroupInvitationIdTest {

    @Test
    void rejects_blank_value() {
        assertThrows(IllegalArgumentException.class, () -> GroupInvitationId.of("   "));
    }
}
