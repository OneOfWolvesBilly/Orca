package io.github.oneofwolvesbilly.orca.organization.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies invite-member invariants and pending invitation state. */
class GroupInviteMemberTest {

    @Test
    void inviteMember_creates_pending_invitation_when_inviter_is_admin() {
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

        assertEquals(InvitationStatus.PENDING, invitation.status());
        assertEquals("g-1", invitation.groupId().value());
        assertEquals("user-1", invitation.inviteeUserId().value());
        assertEquals(GroupRole.MEMBER, invitation.intendedRole());

        assertTrue(group.hasPendingInvitationFor(UserId.of("user-1")));
    }

    @Test
    void inviteMember_rejects_blank_invitee_user_id() {
        var group = Group.create(
                GroupId.of("g-1"),
                GroupName.of("Team A"),
                null,
                UserId.of("admin-1")
        );

        // Value Object invariant: UserId must not be blank.
        assertThrows(IllegalArgumentException.class, () ->
                group.inviteMember(
                        UserId.of("admin-1"),
                        UserId.of(""),
                        GroupRole.MEMBER
                )
        );
    }

    @Test
    void inviteMember_rejects_when_inviter_is_not_admin() {
        var group = Group.create(
                GroupId.of("g-1"),
                GroupName.of("Team A"),
                null,
                UserId.of("admin-1")
        );

        group.addMember(UserId.of("member-1"), GroupRole.MEMBER);

        var ex = assertThrows(DomainException.class, () ->
                group.inviteMember(
                        UserId.of("member-1"),
                        UserId.of("user-1"),
                        GroupRole.MEMBER
                )
        );

        assertEquals(DomainError.INVITER_NOT_GROUP_ADMIN, ex.error());
    }

    @Test
    void inviteMember_rejects_duplicate_pending_invitation() {
        var group = Group.create(
                GroupId.of("g-1"),
                GroupName.of("Team A"),
                null,
                UserId.of("admin-1")
        );

        group.inviteMember(UserId.of("admin-1"), UserId.of("user-1"), GroupRole.MEMBER);

        var ex = assertThrows(DomainException.class, () ->
                group.inviteMember(UserId.of("admin-1"), UserId.of("user-1"), GroupRole.MEMBER)
        );

        assertEquals(DomainError.DUPLICATE_PENDING_INVITATION, ex.error());
    }

    @Test
    void inviteMember_rejects_when_invitee_is_already_member() {
        var group = Group.create(
                GroupId.of("g-1"),
                GroupName.of("Team A"),
                null,
                UserId.of("admin-1")
        );

        group.addMember(UserId.of("user-1"), GroupRole.MEMBER);

        var ex = assertThrows(DomainException.class, () ->
                group.inviteMember(UserId.of("admin-1"), UserId.of("user-1"), GroupRole.MEMBER)
        );

        assertEquals(DomainError.INVITEE_ALREADY_MEMBER, ex.error());
    }
}
