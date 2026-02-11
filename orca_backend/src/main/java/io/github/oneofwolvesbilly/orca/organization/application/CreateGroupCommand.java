package io.github.oneofwolvesbilly.orca.organization.application;

import io.github.oneofwolvesbilly.orca.organization.domain.UserId;

import java.util.Objects;

/** Carries inputs for group creation. */
public record CreateGroupCommand(UserId creatorUserId, String name, String description) {

    public CreateGroupCommand {
        Objects.requireNonNull(creatorUserId, "creatorUserId");
        Objects.requireNonNull(name, "name");
        // description may be null
    }
}