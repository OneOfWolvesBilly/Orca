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

class AcceptInvitationUseCaseTest {

    @Test
    void handle_accepts_invitation_and_persists_group() {
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

        assertTrue(repo.findById(groupId).isPresent());
    }
}