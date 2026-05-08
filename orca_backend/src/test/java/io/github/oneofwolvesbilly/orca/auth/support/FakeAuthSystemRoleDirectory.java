package io.github.oneofwolvesbilly.orca.auth.support;

import io.github.oneofwolvesbilly.orca.auth.application.AuthSystemRoleDirectory;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthSystemRole;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedUserId;

import java.util.HashSet;
import java.util.Set;

public final class FakeAuthSystemRoleDirectory implements AuthSystemRoleDirectory {

    private final Set<RoleAssignment> assignments = new HashSet<>();

    public FakeAuthSystemRoleDirectory grant(String authenticatedUserId, AuthSystemRole role) {
        assignments.add(new RoleAssignment(AuthenticatedUserId.of(authenticatedUserId), role));
        return this;
    }

    @Override
    public boolean hasRole(AuthenticatedUserId authenticatedUserId, AuthSystemRole role) {
        return assignments.contains(new RoleAssignment(authenticatedUserId, role));
    }

    private record RoleAssignment(AuthenticatedUserId authenticatedUserId, AuthSystemRole role) {
    }
}
