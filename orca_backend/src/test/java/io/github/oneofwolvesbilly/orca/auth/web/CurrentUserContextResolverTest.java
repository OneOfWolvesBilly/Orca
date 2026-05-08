package io.github.oneofwolvesbilly.orca.auth.web;

import io.github.oneofwolvesbilly.orca.auth.application.EstablishCurrentUserContextUseCase;
import io.github.oneofwolvesbilly.orca.auth.support.FakeRegisteredUserIdentityRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CurrentUserContextResolverTest {

    private static final FakeRegisteredUserIdentityRepository repository =
            new FakeRegisteredUserIdentityRepository().register("user-1");

    private final CurrentUserContextResolver resolver =
            new CurrentUserContextResolver(new EstablishCurrentUserContextUseCase(repository));

    @Test
    void resolve_establishes_context_when_exactly_one_header_value_is_presented() {
        var context = resolver.resolve(List.of("user-1"));

        assertEquals("user-1", context.authenticatedUserId().value());
    }

    @Test
    void resolve_rejects_when_no_header_value_is_presented() {
        assertThrows(UnauthenticatedHttpRequestException.class, () ->
                resolver.resolve(List.of())
        );
    }

    @Test
    void resolve_rejects_when_blank_header_value_is_presented() {
        assertThrows(UnauthenticatedHttpRequestException.class, () ->
                resolver.resolve(List.of(" "))
        );
    }

    @Test
    void resolve_rejects_when_more_than_one_header_value_is_presented() {
        assertThrows(UnauthenticatedHttpRequestException.class, () ->
                resolver.resolve(List.of("user-1", "user-2"))
        );
    }

    @Test
    void resolve_rejects_when_header_value_is_not_registered() {
        assertThrows(UnauthenticatedHttpRequestException.class, () ->
                resolver.resolve(List.of("missing-user"))
        );
    }
}
