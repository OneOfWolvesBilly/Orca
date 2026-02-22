package io.github.oneofwolvesbilly.orca.organization.domain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies accept-invitation invariants and status transition behavior. */
class GroupAcceptInvitationTest {

    @Test
    void acceptInvitation_marks_invitation_accepted_and_adds_member_atomically() {
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

        // The spec 03 input is invitation id. Domain should expose it.
        var invitationId = invitation.id();

        group.acceptInvitation(invitationId, UserId.of("user-1"));

        assertEquals(InvitationStatus.ACCEPTED, group.getInvitation(invitationId).status());

        assertTrue(group.isMember(UserId.of("user-1")));
        assertEquals(GroupRole.MEMBER, group.getMember(UserId.of("user-1")).role());

        assertFalse(group.hasPendingInvitationFor(UserId.of("user-1")));
    }

    @Test
    void acceptInvitation_rejects_when_invitation_does_not_exist() {
        var group = Group.create(
                GroupId.of("g-1"),
                GroupName.of("Team A"),
                null,
                UserId.of("admin-1")
        );

        assertThrows(DomainException.class, () ->
                group.acceptInvitation(GroupInvitationId.of("inv-404"), UserId.of("user-1"))
        );
    }

    @Test
    void acceptInvitation_rejects_when_invitation_status_is_not_pending() {
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

        group.acceptInvitation(invitationId, UserId.of("user-1"));

        var ex = assertThrows(DomainException.class, () ->
                group.acceptInvitation(invitationId, UserId.of("user-1"))
        );

        // If you already have a dedicated error, assert it here.
        // Otherwise keep this test only on exception type.
        // assertEquals(DomainError.INVITATION_STATUS_NOT_PENDING, ex.error());
        assertNotNull(ex);
    }

    @Test
    void acceptInvitation_rejects_when_accepting_user_is_not_invitee() {
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
                group.acceptInvitation(invitationId, UserId.of("other-1"))
        );

        // If you already have a dedicated error, assert it here.
        // assertEquals(DomainError.ACCEPTOR_NOT_INVITEE, ex.error());
        assertNotNull(ex);
    }

    @Test
    void acceptInvitation_rejects_when_invitee_is_already_member() {
        var group = Group.create(
                GroupId.of("g-1"),
                GroupName.of("Team A"),
                null,
                UserId.of("admin-1")
        );

        // Create a pending invitation for user-1
        var invitation = group.inviteMember(
                UserId.of("admin-1"),
                UserId.of("user-1"),
                GroupRole.MEMBER
        );

        // Edge state: invitee becomes a member before accepting the invitation
        group.addMember(UserId.of("user-1"), GroupRole.MEMBER);

        var ex = assertThrows(DomainException.class, () ->
                group.acceptInvitation(invitation.id(), UserId.of("user-1"))
        );

        assertEquals(DomainError.INVITEE_ALREADY_MEMBER, ex.error());
    }
}
