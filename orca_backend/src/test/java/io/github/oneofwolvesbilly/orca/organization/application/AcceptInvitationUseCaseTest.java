package io.github.oneofwolvesbilly.orca.organization.application;

import io.github.oneofwolvesbilly.orca.organization.domain.Group;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupId;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupInvitationId;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupName;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupRole;
import io.github.oneofwolvesbilly.orca.organization.domain.UserId;
import io.github.oneofwolvesbilly.orca.organization.infrastructure.inmemory.InMemoryGroupRepository;
import io.github.oneofwolvesbilly.orca.organization.infrastructure.inmemory.InMemoryRegisteredUserDirectory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AcceptInvitationUseCaseTest {

    @Test
    void handle_accepts_invitation_adds_member_removes_pending_invitation_and_saves_once() {
        var repo = new InMemoryGroupRepository();
        var users = new InMemoryRegisteredUserDirectory();

        var groupId = GroupId.of("g-1");
        var adminId = UserId.of("user-admin");
        var inviteeId = UserId.of("user-1");
        users.register(inviteeId);

        Group group = Group.create(groupId, GroupName.of("Team A"), null, adminId);
        repo.save(group);

        var inviteUseCase = new InviteMemberUseCase(repo, users);
        var inviteResult = inviteUseCase.handle(
                new InviteMemberCommand(groupId, adminId, inviteeId, GroupRole.MEMBER)
        );

        var acceptUseCase = new AcceptInvitationUseCase(repo);
        int savesBeforeAccept = repo.savedGroups().size();

        acceptUseCase.handle(new AcceptInvitationCommand(inviteeId, inviteResult.invitationId()));

        Group saved = repo.findById(groupId).orElseThrow();
        assertEquals(savesBeforeAccept + 1, repo.savedGroups().size());
        assertFalse(saved.hasPendingInvitationFor(inviteeId));
        assertTrue(saved.members().stream()
                .anyMatch(member -> member.userId().equals(inviteeId) && member.role() == GroupRole.MEMBER));
    }

    @Test
    void handle_rejects_unknown_invitation_without_saving_or_changing_group_state() {
        var repo = new InMemoryGroupRepository();
        var users = new InMemoryRegisteredUserDirectory();

        var groupId = GroupId.of("g-1");
        var adminId = UserId.of("user-admin");
        var inviteeId = UserId.of("user-1");
        users.register(inviteeId);

        Group group = Group.create(groupId, GroupName.of("Team A"), null, adminId);
        repo.save(group);

        var inviteUseCase = new InviteMemberUseCase(repo, users);
        inviteUseCase.handle(new InviteMemberCommand(groupId, adminId, inviteeId, GroupRole.MEMBER));

        Group before = repo.findById(groupId).orElseThrow();
        int savesBeforeFailure = repo.savedGroups().size();
        int membersBeforeFailure = before.members().size();

        var acceptUseCase = new AcceptInvitationUseCase(repo);

        assertThrows(IllegalArgumentException.class, () ->
                acceptUseCase.handle(new AcceptInvitationCommand(inviteeId, GroupInvitationId.of("missing-invitation")))
        );

        Group after = repo.findById(groupId).orElseThrow();
        assertEquals(savesBeforeFailure, repo.savedGroups().size());
        assertEquals(membersBeforeFailure, after.members().size());
        assertTrue(after.hasPendingInvitationFor(inviteeId));
    }

    @Test
    void handle_rejects_non_invitee_actor_without_saving_or_changing_group_state() {
        var repo = new InMemoryGroupRepository();
        var users = new InMemoryRegisteredUserDirectory();

        var groupId = GroupId.of("g-1");
        var adminId = UserId.of("user-admin");
        var inviteeId = UserId.of("user-1");
        var otherUserId = UserId.of("user-2");
        users.register(inviteeId);

        Group group = Group.create(groupId, GroupName.of("Team A"), null, adminId);
        repo.save(group);

        var inviteUseCase = new InviteMemberUseCase(repo, users);
        var inviteResult = inviteUseCase.handle(
                new InviteMemberCommand(groupId, adminId, inviteeId, GroupRole.MEMBER)
        );

        Group before = repo.findById(groupId).orElseThrow();
        int savesBeforeFailure = repo.savedGroups().size();
        int membersBeforeFailure = before.members().size();

        var acceptUseCase = new AcceptInvitationUseCase(repo);

        assertThrows(RuntimeException.class, () ->
                acceptUseCase.handle(new AcceptInvitationCommand(otherUserId, inviteResult.invitationId()))
        );

        Group after = repo.findById(groupId).orElseThrow();
        assertEquals(savesBeforeFailure, repo.savedGroups().size());
        assertEquals(membersBeforeFailure, after.members().size());
        assertTrue(after.hasPendingInvitationFor(inviteeId));
    }

    @Test
    void handle_rejects_non_pending_invitation_without_saving_or_changing_group_state() {
        var repo = new InMemoryGroupRepository();
        var users = new InMemoryRegisteredUserDirectory();

        var groupId = GroupId.of("g-1");
        var adminId = UserId.of("user-admin");
        var inviteeId = UserId.of("user-1");
        users.register(inviteeId);

        Group group = Group.create(groupId, GroupName.of("Team A"), null, adminId);
        repo.save(group);

        var inviteUseCase = new InviteMemberUseCase(repo, users);
        var inviteResult = inviteUseCase.handle(
                new InviteMemberCommand(groupId, adminId, inviteeId, GroupRole.MEMBER)
        );

        var acceptUseCase = new AcceptInvitationUseCase(repo);
        acceptUseCase.handle(new AcceptInvitationCommand(inviteeId, inviteResult.invitationId()));

        Group before = repo.findById(groupId).orElseThrow();
        int savesBeforeFailure = repo.savedGroups().size();
        int membersBeforeFailure = before.members().size();

        assertThrows(RuntimeException.class, () ->
                acceptUseCase.handle(new AcceptInvitationCommand(inviteeId, inviteResult.invitationId()))
        );

        Group after = repo.findById(groupId).orElseThrow();
        assertEquals(savesBeforeFailure, repo.savedGroups().size());
        assertEquals(membersBeforeFailure, after.members().size());
        assertFalse(after.hasPendingInvitationFor(inviteeId));
    }
}
