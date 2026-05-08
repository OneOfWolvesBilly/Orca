package io.github.oneofwolvesbilly.orca.auth.application;

import io.github.oneofwolvesbilly.orca.auth.domain.AuthSystemRole;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedUserId;
import io.github.oneofwolvesbilly.orca.auth.support.FakeAuthSystemRoleDirectory;
import io.github.oneofwolvesbilly.orca.auth.support.FakeRegisteredUserIdentityRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProvisionRegisteredUserIdentityUseCaseTest {

    @Test
    void handle_provisions_regular_registered_user_identity_when_actor_is_it_admin() {
        var identities = new FakeRegisteredUserIdentityRepository().register("admin");
        var roles = new FakeAuthSystemRoleDirectory().grant("admin", AuthSystemRole.IT_ADMIN);
        var useCase = new ProvisionRegisteredUserIdentityUseCase(identities, roles);

        useCase.handle(new ProvisionRegisteredUserIdentityCommand(
                AuthenticatedUserId.of("admin"),
                "user-1"
        ));

        assertTrue(identities.exists(AuthenticatedUserId.of("user-1")));
        assertFalse(roles.hasRole(AuthenticatedUserId.of("user-1"), AuthSystemRole.IT_ADMIN));
    }

    @Test
    void handle_rejects_when_actor_lacks_it_admin_role() {
        var identities = new FakeRegisteredUserIdentityRepository()
                .register("admin")
                .register("user-1");
        var useCase = new ProvisionRegisteredUserIdentityUseCase(identities, new FakeAuthSystemRoleDirectory());

        assertThrows(UnauthorizedAuthOperationException.class, () ->
                useCase.handle(new ProvisionRegisteredUserIdentityCommand(
                        AuthenticatedUserId.of("admin"),
                        "user-2"
                ))
        );

        assertFalse(identities.exists(AuthenticatedUserId.of("user-2")));
    }

    @Test
    void handle_rejects_when_requested_user_id_is_already_registered() {
        var identities = new FakeRegisteredUserIdentityRepository()
                .register("admin")
                .register("user-1");
        var roles = new FakeAuthSystemRoleDirectory().grant("admin", AuthSystemRole.IT_ADMIN);
        var useCase = new ProvisionRegisteredUserIdentityUseCase(identities, roles);

        assertThrows(RegisteredUserIdentityAlreadyExistsException.class, () ->
                useCase.handle(new ProvisionRegisteredUserIdentityCommand(
                        AuthenticatedUserId.of("admin"),
                        "user-1"
                ))
        );
    }

    @Test
    void handle_rejects_blank_requested_user_id() {
        var identities = new FakeRegisteredUserIdentityRepository().register("admin");
        var roles = new FakeAuthSystemRoleDirectory().grant("admin", AuthSystemRole.IT_ADMIN);
        var useCase = new ProvisionRegisteredUserIdentityUseCase(identities, roles);

        assertThrows(IllegalArgumentException.class, () ->
                useCase.handle(new ProvisionRegisteredUserIdentityCommand(
                        AuthenticatedUserId.of("admin"),
                        " "
                ))
        );
    }
}
