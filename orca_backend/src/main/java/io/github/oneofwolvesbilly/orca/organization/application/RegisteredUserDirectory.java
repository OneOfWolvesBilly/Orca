package io.github.oneofwolvesbilly.orca.organization.application;

import io.github.oneofwolvesbilly.orca.organization.domain.UserId;

/** Resolves whether a userId belongs to an existing registered user. */
public interface RegisteredUserDirectory {
    boolean exists(UserId userId);
}