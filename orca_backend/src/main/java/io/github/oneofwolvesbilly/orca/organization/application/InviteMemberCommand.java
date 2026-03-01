package io.github.oneofwolvesbilly.orca.organization.application;

import io.github.oneofwolvesbilly.orca.organization.domain.GroupId;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupRole;
import io.github.oneofwolvesbilly.orca.organization.domain.UserId;

import java.util.Objects;

/** Carries inputs for inviting a member to a group. */
public record InviteMemberCommand(
        GroupId groupId,
        UserId inviterUserId,
        UserId inviteeUserId,
        GroupRole intendedRole
) {
    public InviteMemberCommand {
        Objects.requireNonNull(groupId, "groupId");
        Objects.requireNonNull(inviterUserId, "inviterUserId");
        Objects.requireNonNull(inviteeUserId, "inviteeUserId");
        Objects.requireNonNull(intendedRole, "intendedRole");
    }
}