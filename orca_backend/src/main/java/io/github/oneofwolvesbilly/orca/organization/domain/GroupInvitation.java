package io.github.oneofwolvesbilly.orca.organization.domain;

import java.util.Objects;

/**
 * Represents a pending invitation for a registered user to join a group.
 *
 * Spec 02 - Invite Member
 * - Then: create a new invitation in PENDING status
 * - Then: associate invitation with the group
 * - Then: target invitee userId and include intended role
 */
public final class GroupInvitation {

    private final GroupInvitationId id;
    private final GroupId groupId;
    private final UserId inviteeUserId;
    private final GroupRole intendedRole;
    private final InvitationStatus status;

    public GroupInvitation(GroupInvitationId id,
                           GroupId groupId,
                           UserId inviteeUserId,
                           GroupRole intendedRole,
                           InvitationStatus status) {
        this.id = Objects.requireNonNull(id, "id");
        this.groupId = Objects.requireNonNull(groupId, "groupId");
        this.inviteeUserId = Objects.requireNonNull(inviteeUserId, "inviteeUserId");
        this.intendedRole = Objects.requireNonNull(intendedRole, "intendedRole");
        this.status = Objects.requireNonNull(status, "status");
    }

    public GroupInvitationId id() {
        return id;
    }

    public GroupId groupId() {
        return groupId;
    }

    public UserId inviteeUserId() {
        return inviteeUserId;
    }

    public GroupRole intendedRole() {
        return intendedRole;
    }

    public InvitationStatus status() {
        return status;
    }
}
