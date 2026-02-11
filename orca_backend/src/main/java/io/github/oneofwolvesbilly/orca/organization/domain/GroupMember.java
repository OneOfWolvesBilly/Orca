package io.github.oneofwolvesbilly.orca.organization.domain;

import java.util.Objects;

/** Binds a user to a role within a group. */
public record GroupMember(UserId userId, GroupRole role) {

    public GroupMember {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(role, "role");
    }

    /** Creates a group member. */
    public static GroupMember of(UserId userId, GroupRole role) {
        return new GroupMember(userId, role);
    }
}