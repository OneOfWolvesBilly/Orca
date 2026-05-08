package io.github.oneofwolvesbilly.orca.auth.application;

import io.github.oneofwolvesbilly.orca.auth.support.FakeRegisteredUserIdentityRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EstablishCurrentUserContextUseCaseTest {

    @Test
    void handle_establishes_context_when_exactly_one_authenticated_user_is_presented() {
        var repository = new FakeRegisteredUserIdentityRepository().register("user-1");
        var useCase = new EstablishCurrentUserContextUseCase(repository);

        var context = useCase.handle(new EstablishCurrentUserContextCommand(List.of("user-1")));

        assertEquals("user-1", context.authenticatedUserId().value());
    }

    @Test
    void handle_rejects_when_no_authenticated_user_is_presented() {
        var useCase = new EstablishCurrentUserContextUseCase(new FakeRegisteredUserIdentityRepository());

        assertThrows(UnauthenticatedOperationException.class, () ->
                useCase.handle(new EstablishCurrentUserContextCommand(List.of()))
        );
    }

    @Test
    void handle_rejects_when_more_than_one_authenticated_user_is_presented() {
        var useCase = new EstablishCurrentUserContextUseCase(new FakeRegisteredUserIdentityRepository());

        assertThrows(AmbiguousAuthenticatedUserException.class, () ->
                useCase.handle(new EstablishCurrentUserContextCommand(List.of("user-1", "user-2")))
        );
    }

    @Test
    void handle_rejects_when_presented_user_id_is_blank() {
        var useCase = new EstablishCurrentUserContextUseCase(new FakeRegisteredUserIdentityRepository());

        assertThrows(IllegalArgumentException.class, () ->
                useCase.handle(new EstablishCurrentUserContextCommand(List.of(" ")))
        );
    }

    @Test
    void handle_rejects_when_presented_user_id_is_not_registered() {
        var useCase = new EstablishCurrentUserContextUseCase(new FakeRegisteredUserIdentityRepository());

        assertThrows(UnauthenticatedOperationException.class, () ->
                useCase.handle(new EstablishCurrentUserContextCommand(List.of("missing-user")))
        );
    }
}
