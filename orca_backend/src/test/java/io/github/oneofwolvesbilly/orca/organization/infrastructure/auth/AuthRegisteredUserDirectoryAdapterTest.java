package io.github.oneofwolvesbilly.orca.organization.infrastructure.auth;

import io.github.oneofwolvesbilly.orca.auth.support.FakeRegisteredUserIdentityRepository;
import io.github.oneofwolvesbilly.orca.organization.domain.UserId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthRegisteredUserDirectoryAdapterTest {

    @Test
    void exists_uses_auth_registered_user_identity_source() {
        var repository = new FakeRegisteredUserIdentityRepository().register("user-1");
        var adapter = new AuthRegisteredUserDirectoryAdapter(repository);

        assertTrue(adapter.exists(UserId.of("user-1")));
        assertFalse(adapter.exists(UserId.of("missing-user")));
    }
}
