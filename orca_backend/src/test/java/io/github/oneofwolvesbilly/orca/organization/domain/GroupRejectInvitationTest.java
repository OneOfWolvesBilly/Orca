package io.github.oneofwolvesbilly.orca.organization.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies reject-invitation invariants and status transition behavior. */
class GroupRejectInvitationTest {

    @Test
    void rejectInvitation_marks_invitation_rejected_and_removes_pending_link_atomically() {
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

        group.rejectInvitation(invitationId, UserId.of("user-1"));

        assertEquals(InvitationStatus.REJECTED, group.getInvitation(invitationId).status());
        assertFalse(group.hasPendingInvitationFor(UserId.of("user-1")));
        assertFalse(group.isMember(UserId.of("user-1")));
    }

    @Test
    void rejectInvitation_rejects_when_invitation_does_not_exist() {
        var group = Group.create(
                GroupId.of("g-1"),
                GroupName.of("Team A"),
                null,
                UserId.of("admin-1")
        );

        assertThrows(DomainException.class, () ->
                group.rejectInvitation(GroupInvitationId.of("inv-404"), UserId.of("user-1"))
        );
    }

    @Test
    void rejectInvitation_rejects_when_invitation_status_is_not_pending() {
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
                group.rejectInvitation(invitationId, UserId.of("user-1"))
        );

        assertEquals(DomainError.INVITATION_NOT_PENDING, ex.error());
    }

    @Test
    void rejectInvitation_rejects_when_rejecting_user_is_not_invitee() {
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

        var ex = assertThrows(DomainException.class, () ->
                group.rejectInvitation(invitationId, UserId.of("other-1"))
        );

        assertEquals(DomainError.INVITATION_REJECTOR_MISMATCH, ex.error());
    }

    @Test
    void rejectInvitation_rejects_when_invitee_is_already_member() {
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

        // Edge state: invitee becomes a member before rejecting the invitation
        group.addMember(UserId.of("user-1"), GroupRole.MEMBER);

        var ex = assertThrows(DomainException.class, () ->
                group.rejectInvitation(invitation.id(), UserId.of("user-1"))
        );

        assertEquals(DomainError.INVITEE_ALREADY_MEMBER, ex.error());
    }
}
