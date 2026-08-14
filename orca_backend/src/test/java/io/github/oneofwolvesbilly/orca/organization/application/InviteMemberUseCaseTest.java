package io.github.oneofwolvesbilly.orca.organization.application;

import io.github.oneofwolvesbilly.orca.organization.domain.Group;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupId;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupName;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupRole;
import io.github.oneofwolvesbilly.orca.organization.domain.UserId;
import io.github.oneofwolvesbilly.orca.organization.infrastructure.inmemory.InMemoryGroupRepository;
import io.github.oneofwolvesbilly.orca.organization.infrastructure.inmemory.InMemoryRegisteredUserDirectory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InviteMemberUseCaseTest {

    @Test
    void handle_invites_member_persists_group_and_invitation_lookup_atomically() {
        var repo = new InMemoryGroupRepository();
        var users = new InMemoryRegisteredUserDirectory();

        var groupId = GroupId.of("g-1");
        var adminId = UserId.of("user-admin");
        var inviteeId = UserId.of("user-1");

        users.register(inviteeId);

        Group group = Group.create(groupId, GroupName.of("Team A"), null, adminId);
        repo.save(group);

        var useCase = new InviteMemberUseCase(repo, users);

        InviteMemberResult result = useCase.handle(
                new InviteMemberCommand(groupId, adminId, inviteeId, GroupRole.MEMBER)
        );

        assertNotNull(result.invitationId());
        assertEquals(1, repo.indexedInvitations().size());
        assertTrue(repo.findByInvitationId(result.invitationId()).isPresent());
        assertEquals("g-1", repo.findByInvitationId(result.invitationId()).get().id().value());
    }

    @Test
    void spec02_handle_rejects_group_not_found_without_saving_indexing_or_changing_group_state() {
        var repo = new InMemoryGroupRepository();
        var users = new InMemoryRegisteredUserDirectory();

        var groupId = GroupId.of("g-missing");
        var adminId = UserId.of("user-admin");
        var inviteeId = UserId.of("user-1");
        users.register(inviteeId);

        var useCase = new InviteMemberUseCase(repo, users);

        var failure = assertThrows(OrganizationApplicationFailure.class, () ->
                useCase.handle(new InviteMemberCommand(groupId, adminId, inviteeId, GroupRole.MEMBER))
        );
        assertEquals(OrganizationFailureCategory.NOT_FOUND, failure.category());

        assertTrue(repo.savedGroups().isEmpty());
        assertTrue(repo.indexedInvitations().isEmpty());
        assertTrue(repo.findById(groupId).isEmpty());
    }

    @Test
    void spec02_handle_rejects_inviter_who_is_not_group_admin_without_saving_indexing_or_changing_group_state() {
        var repo = new InMemoryGroupRepository();
        var users = new InMemoryRegisteredUserDirectory();

        var groupId = GroupId.of("g-1");
        var adminId = UserId.of("user-admin");
        var nonAdminInviterId = UserId.of("user-non-admin");
        var inviteeId = UserId.of("user-1");

        users.register(inviteeId);

        Group group = Group.create(groupId, GroupName.of("Team A"), null, adminId);
        repo.save(group);

        Group before = repo.findById(groupId).orElseThrow();
        int savesBeforeFailure = repo.savedGroups().size();
        int indexesBeforeFailure = repo.indexedInvitations().size();
        int membersBeforeFailure = before.members().size();
        boolean hadPendingBeforeFailure = before.hasPendingInvitationFor(inviteeId);

        var useCase = new InviteMemberUseCase(repo, users);

        var failure = assertThrows(OrganizationApplicationFailure.class, () ->
                useCase.handle(new InviteMemberCommand(groupId, nonAdminInviterId, inviteeId, GroupRole.MEMBER))
        );
        assertEquals(OrganizationFailureCategory.FORBIDDEN, failure.category());

        assertFailureDidNotPersistIndexOrChangeGroup(
                repo,
                groupId,
                inviteeId,
                savesBeforeFailure,
                indexesBeforeFailure,
                membersBeforeFailure,
                hadPendingBeforeFailure
        );
    }

    @Test
    void spec02_handle_rejects_duplicate_pending_invitation_without_saving_indexing_or_changing_group_state() {
        var repo = new InMemoryGroupRepository();
        var users = new InMemoryRegisteredUserDirectory();

        var groupId = GroupId.of("g-1");
        var adminId = UserId.of("user-admin");
        var inviteeId = UserId.of("user-1");

        users.register(inviteeId);

        Group group = Group.create(groupId, GroupName.of("Team A"), null, adminId);
        repo.save(group);

        var useCase = new InviteMemberUseCase(repo, users);
        useCase.handle(new InviteMemberCommand(groupId, adminId, inviteeId, GroupRole.MEMBER));

        Group before = repo.findById(groupId).orElseThrow();
        int savesBeforeFailure = repo.savedGroups().size();
        int indexesBeforeFailure = repo.indexedInvitations().size();
        int membersBeforeFailure = before.members().size();
        boolean hadPendingBeforeFailure = before.hasPendingInvitationFor(inviteeId);

        var failure = assertThrows(OrganizationApplicationFailure.class, () ->
                useCase.handle(new InviteMemberCommand(groupId, adminId, inviteeId, GroupRole.MEMBER))
        );
        assertEquals(OrganizationFailureCategory.APPLICATION_REJECTED, failure.category());

        assertFailureDidNotPersistIndexOrChangeGroup(
                repo,
                groupId,
                inviteeId,
                savesBeforeFailure,
                indexesBeforeFailure,
                membersBeforeFailure,
                hadPendingBeforeFailure
        );
    }

    @Test
    void spec02_handle_rejects_invitee_already_member_without_saving_indexing_or_changing_group_state() {
        var repo = new InMemoryGroupRepository();
        var users = new InMemoryRegisteredUserDirectory();

        var groupId = GroupId.of("g-1");
        var adminId = UserId.of("user-admin");

        users.register(adminId);

        Group group = Group.create(groupId, GroupName.of("Team A"), null, adminId);
        repo.save(group);

        Group before = repo.findById(groupId).orElseThrow();
        int savesBeforeFailure = repo.savedGroups().size();
        int indexesBeforeFailure = repo.indexedInvitations().size();
        int membersBeforeFailure = before.members().size();
        boolean hadPendingBeforeFailure = before.hasPendingInvitationFor(adminId);

        var useCase = new InviteMemberUseCase(repo, users);

        var failure = assertThrows(OrganizationApplicationFailure.class, () ->
                useCase.handle(new InviteMemberCommand(groupId, adminId, adminId, GroupRole.MEMBER))
        );
        assertEquals(OrganizationFailureCategory.APPLICATION_REJECTED, failure.category());

        assertFailureDidNotPersistIndexOrChangeGroup(
                repo,
                groupId,
                adminId,
                savesBeforeFailure,
                indexesBeforeFailure,
                membersBeforeFailure,
                hadPendingBeforeFailure
        );
    }

    @Test
    void spec02_handle_rejects_invitee_user_does_not_exist_without_saving_indexing_or_changing_group_state() {
        var repo = new InMemoryGroupRepository();
        var users = new InMemoryRegisteredUserDirectory();

        var groupId = GroupId.of("g-1");
        var adminId = UserId.of("user-admin");
        var inviteeId = UserId.of("user-missing");

        Group group = Group.create(groupId, GroupName.of("Team A"), null, adminId);
        repo.save(group);

        var useCase = new InviteMemberUseCase(repo, users);

        Group before = repo.findById(groupId).orElseThrow();
        int savesBeforeFailure = repo.savedGroups().size();
        int indexesBeforeFailure = repo.indexedInvitations().size();
        int membersBeforeFailure = before.members().size();
        boolean hadPendingBeforeFailure = before.hasPendingInvitationFor(inviteeId);

        var failure = assertThrows(OrganizationApplicationFailure.class, () ->
                useCase.handle(new InviteMemberCommand(groupId, adminId, inviteeId, GroupRole.MEMBER))
        );
        assertEquals(OrganizationFailureCategory.APPLICATION_REJECTED, failure.category());

        assertFailureDidNotPersistIndexOrChangeGroup(
                repo,
                groupId,
                inviteeId,
                savesBeforeFailure,
                indexesBeforeFailure,
                membersBeforeFailure,
                hadPendingBeforeFailure
        );
    }

    @Test
    void unexpected_dependency_failure_is_not_reclassified() {
        var repo = new InMemoryGroupRepository();
        RegisteredUserDirectory failingDirectory = userId -> {
            throw new IllegalStateException("directory unavailable");
        };
        var useCase = new InviteMemberUseCase(repo, failingDirectory);

        var failure = assertThrows(IllegalStateException.class, () -> useCase.handle(new InviteMemberCommand(
                GroupId.of("g-1"),
                UserId.of("admin"),
                UserId.of("invitee"),
                GroupRole.MEMBER
        )));

        assertEquals("directory unavailable", failure.getMessage());
    }

    private static void assertFailureDidNotPersistIndexOrChangeGroup(
            InMemoryGroupRepository repo,
            GroupId groupId,
            UserId inviteeId,
            int expectedSaves,
            int expectedIndexes,
            int expectedMembers,
            boolean expectedPending
    ) {
        Group after = repo.findById(groupId).orElseThrow();
        assertEquals(expectedSaves, repo.savedGroups().size());
        assertEquals(expectedIndexes, repo.indexedInvitations().size());
        assertEquals(expectedMembers, after.members().size());
        assertEquals(expectedPending, after.hasPendingInvitationFor(inviteeId));
    }
}
