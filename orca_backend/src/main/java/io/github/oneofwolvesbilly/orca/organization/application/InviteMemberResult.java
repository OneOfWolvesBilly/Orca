package io.github.oneofwolvesbilly.orca.organization.application;

import io.github.oneofwolvesbilly.orca.organization.domain.GroupInvitationId;

import java.util.Objects;

/** Represents the outcome of inviting a member. */
public record InviteMemberResult(GroupInvitationId invitationId) {
    public InviteMemberResult {
        Objects.requireNonNull(invitationId, "invitationId");
    }
}