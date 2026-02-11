package io.github.oneofwolvesbilly.orca.organization.application;

import io.github.oneofwolvesbilly.orca.organization.domain.GroupId;

import java.util.Objects;

/** Carries outputs for group creation. */
public record CreateGroupResult(GroupId groupId) {

    public CreateGroupResult {
        Objects.requireNonNull(groupId, "groupId");
    }

    /** Creates a result for a newly created group. */
    public static CreateGroupResult created(GroupId groupId) {
        return new CreateGroupResult(groupId);
    }
}