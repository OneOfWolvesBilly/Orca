package io.github.oneofwolvesbilly.orca.organization.web;

import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedUserId;
import io.github.oneofwolvesbilly.orca.auth.domain.CurrentUserContext;
import io.github.oneofwolvesbilly.orca.organization.application.AcceptInvitationUseCase;
import io.github.oneofwolvesbilly.orca.organization.application.CreateGroupUseCase;
import io.github.oneofwolvesbilly.orca.organization.application.InviteMemberUseCase;
import io.github.oneofwolvesbilly.orca.organization.application.RejectInvitationUseCase;
import io.github.oneofwolvesbilly.orca.organization.application.RevokeInvitationUseCase;
import io.github.oneofwolvesbilly.orca.organization.application.GroupRepository;
import io.github.oneofwolvesbilly.orca.organization.domain.Group;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupId;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupInvitationId;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrganizationCommandControllerValidationTest {

    private final CountingGroupRepository repository = new CountingGroupRepository();
    private int generatedIdCount;
    private int registeredUserLookupCount;
    private final CreateGroupUseCase create = new CreateGroupUseCase(
            repository,
            event -> { },
            () -> {
                generatedIdCount++;
                return GroupId.of("generated");
            }
    );
    private final InviteMemberUseCase invite = new InviteMemberUseCase(
            repository,
            userId -> {
                registeredUserLookupCount++;
                return true;
            }
    );
    private final AcceptInvitationUseCase accept = new AcceptInvitationUseCase(repository);
    private final RejectInvitationUseCase reject = new RejectInvitationUseCase(repository);
    private final RevokeInvitationUseCase revoke = new RevokeInvitationUseCase(repository);
    private final OrganizationCommandController controller =
            new OrganizationCommandController(create, invite, accept, reject, revoke);
    private final CurrentUserContext actor =
            CurrentUserContext.establish(AuthenticatedUserId.of("actor"));

    @Test
    void rejects_missing_or_blank_create_fields_before_use_case_execution() {
        assertThrows(IllegalArgumentException.class, () -> controller.createGroup(actor, null));
        assertThrows(IllegalArgumentException.class, () -> controller.createGroup(
                actor,
                new OrganizationCommandController.CreateGroupRequest("   ", null)
        ));

        assertEquals(0, generatedIdCount);
        assertEquals(0, repository.executionCount);
    }

    @Test
    void rejects_blank_group_id_and_missing_or_blank_invite_fields_before_use_case_execution() {
        var valid = new OrganizationCommandController.InviteMemberRequest(
                "invitee",
                io.github.oneofwolvesbilly.orca.organization.domain.GroupRole.MEMBER
        );

        assertThrows(IllegalArgumentException.class, () -> controller.inviteMember(actor, "   ", valid));
        assertThrows(IllegalArgumentException.class, () -> controller.inviteMember(actor, "group", null));
        assertThrows(IllegalArgumentException.class, () -> controller.inviteMember(
                actor,
                "group",
                new OrganizationCommandController.InviteMemberRequest(
                        "   ",
                        io.github.oneofwolvesbilly.orca.organization.domain.GroupRole.MEMBER
                )
        ));

        assertEquals(0, registeredUserLookupCount);
        assertEquals(0, repository.executionCount);
    }

    @Test
    void rejects_blank_invitation_id_for_every_action_before_use_case_execution() {
        var request = new OrganizationCommandController.InvitationActionRequest();

        assertThrows(IllegalArgumentException.class, () -> controller.acceptInvitation(actor, "   ", request));
        assertThrows(IllegalArgumentException.class, () -> controller.rejectInvitation(actor, "   ", request));
        assertThrows(IllegalArgumentException.class, () -> controller.revokeInvitation(actor, "   ", request));

        assertEquals(0, repository.executionCount);
    }

    private static final class CountingGroupRepository implements GroupRepository {
        private int executionCount;

        @Override
        public Optional<Group> findById(GroupId groupId) {
            executionCount++;
            return Optional.empty();
        }

        @Override
        public Optional<Group> findByInvitationId(GroupInvitationId invitationId) {
            executionCount++;
            return Optional.empty();
        }

        @Override
        public void indexInvitation(GroupInvitationId invitationId, GroupId groupId) {
            executionCount++;
        }

        @Override
        public void save(Group group) {
            executionCount++;
        }
    }
}
