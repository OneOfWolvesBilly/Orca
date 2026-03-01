package io.github.oneofwolvesbilly.orca.organization.application;

import io.github.oneofwolvesbilly.orca.organization.domain.GroupInvitationId;
import io.github.oneofwolvesbilly.orca.organization.domain.UserId;

import java.util.Objects;

/** Carries inputs for rejecting an invitation. */
public record RejectInvitationCommand(UserId actorUserId, GroupInvitationId invitationId) {
    public RejectInvitationCommand {
        Objects.requireNonNull(actorUserId, "actorUserId");
        Objects.requireNonNull(invitationId, "invitationId");
    }
}