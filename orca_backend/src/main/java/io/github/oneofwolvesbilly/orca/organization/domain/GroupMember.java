package io.github.oneofwolvesbilly.orca.organization.domain;

public record GroupMember(UserId userId, GroupRole role) {
    public static GroupMember of(UserId userId, GroupRole role) {
        return new GroupMember(userId, role);
    }
}