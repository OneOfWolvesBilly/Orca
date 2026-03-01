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
    void handle_invites_member_persists_group_and_indexes_invitation() {
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
        assertTrue(repo.findByInvitationId(result.invitationId()).isPresent());
        assertEquals("g-1", repo.findByInvitationId(result.invitationId()).get().id().value());
    }

    @Test
    void handle_throws_when_invitee_user_does_not_exist() {
        var repo = new InMemoryGroupRepository();
        var users = new InMemoryRegisteredUserDirectory();

        var groupId = GroupId.of("g-1");
        var adminId = UserId.of("user-admin");
        var inviteeId = UserId.of("user-missing");

        Group group = Group.create(groupId, GroupName.of("Team A"), null, adminId);
        repo.save(group);

        var useCase = new InviteMemberUseCase(repo, users);

        assertThrows(IllegalArgumentException.class, () ->
                useCase.handle(new InviteMemberCommand(groupId, adminId, inviteeId, GroupRole.MEMBER))
        );
    }
}