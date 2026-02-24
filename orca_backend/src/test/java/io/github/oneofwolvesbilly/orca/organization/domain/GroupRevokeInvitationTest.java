package io.github.oneofwolvesbilly.orca.organization.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies revoke-invitation invariants and status transition behavior. */
class GroupRevokeInvitationTest {

    @Test
    void revokeInvitation_marks_invitation_revoked_and_removes_pending_link_atomically() {
        var group = Group.create(
                GroupId.of("g-1"),
                GroupName.of("Team A"),
                null,
                UserId.of("admin-1")
        );

        var invitation = group.inviteMember(
                UserId.of("admin-1"),
                UserId.of("user-1"),
                GroupRole.MEMBER
        );

        var invitationId = invitation.id();

        group.revokeInvitation(invitationId, UserId.of("admin-1"));

        assertEquals(InvitationStatus.REVOKED, group.getInvitation(invitationId).status());
        assertFalse(group.hasPendingInvitationFor(UserId.of("user-1")));
        assertFalse(group.isMember(UserId.of("user-1")));
    }

    @Test
    void revokeInvitation_rejects_when_revoker_is_not_admin() {
        var group = Group.create(
                GroupId.of("g-1"),
                GroupName.of("Team A"),
                null,
                UserId.of("admin-1")
        );

        group.addMember(UserId.of("member-1"), GroupRole.MEMBER);

        var invitation = group.inviteMember(
                UserId.of("admin-1"),
                UserId.of("user-1"),
                GroupRole.MEMBER
        );

        var ex = assertThrows(DomainException.class, () ->
                group.revokeInvitation(invitation.id(), UserId.of("member-1"))
        );

        // Reuse existing error category (minimal change, consistent with invite-member).
        assertEquals(DomainError.INVITER_NOT_GROUP_ADMIN, ex.error());
    }

    @Test
    void revokeInvitation_rejects_when_invitation_does_not_exist() {
        var group = Group.create(
                GroupId.of("g-1"),
                GroupName.of("Team A"),
                null,
                UserId.of("admin-1")
        );

        assertThrows(DomainException.class, () ->
                group.revokeInvitation(GroupInvitationId.of("inv-404"), UserId.of("admin-1"))
        );
    }

    @Test
    void revokeInvitation_rejects_when_invitation_status_is_not_pending() {
        var group = Group.create(
                GroupId.of("g-1"),
                GroupName.of("Team A"),
                null,
                UserId.of("admin-1")
        );

        var invitation = group.inviteMember(
                UserId.of("admin-1"),
                UserId.of("user-1"),
                GroupRole.MEMBER
        );

        var invitationId = invitation.id();

        // make it not pending
        group.acceptInvitation(invitationId, UserId.of("user-1"));

        var ex = assertThrows(DomainException.class, () ->
                group.revokeInvitation(invitationId, UserId.of("admin-1"))
        );

        assertEquals(DomainError.INVITATION_NOT_PENDING, ex.error());
    }
}