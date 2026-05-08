package io.github.oneofwolvesbilly.orca.auth.application;

import io.github.oneofwolvesbilly.orca.auth.domain.AuthSystemRole;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedUserId;

public interface AuthSystemRoleDirectory {

    boolean hasRole(AuthenticatedUserId authenticatedUserId, AuthSystemRole role);
}
