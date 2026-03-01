package io.github.oneofwolvesbilly.orca.organization.application;

import io.github.oneofwolvesbilly.orca.organization.domain.GroupInvitationId;
import io.github.oneofwolvesbilly.orca.organization.domain.UserId;

import java.util.Objects;

/** Carries inputs for accepting an invitation. */
public record AcceptInvitationCommand(UserId actorUserId, GroupInvitationId invitationId) {
    public AcceptInvitationCommand {
        Objects.requireNonNull(actorUserId, "actorUserId");
        Objects.requireNonNull(invitationId, "invitationId");
    }
}