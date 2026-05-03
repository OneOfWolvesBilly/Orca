package io.github.oneofwolvesbilly.orca.auth.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CurrentUserContextTest {

    @Test
    void establish_contains_exactly_one_authenticated_user_id() {
        var context = CurrentUserContext.establish(AuthenticatedUserId.of("user-1"));

        assertEquals("user-1", context.authenticatedUserId().value());
    }
}
