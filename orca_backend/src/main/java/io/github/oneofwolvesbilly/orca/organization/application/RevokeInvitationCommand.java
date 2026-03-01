package io.github.oneofwolvesbilly.orca.organization.application;

import io.github.oneofwolvesbilly.orca.organization.domain.GroupInvitationId;
import io.github.oneofwolvesbilly.orca.organization.domain.UserId;

import java.util.Objects;

/** Carries inputs for revoking an invitation. */
public record RevokeInvitationCommand(UserId actorUserId, GroupInvitationId invitationId) {
    public RevokeInvitationCommand {
        Objects.requireNonNull(actorUserId, "actorUserId");
        Objects.requireNonNull(invitationId, "invitationId");
    }
}