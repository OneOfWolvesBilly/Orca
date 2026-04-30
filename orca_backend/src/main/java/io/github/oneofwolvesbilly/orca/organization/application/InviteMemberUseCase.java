package io.github.oneofwolvesbilly.orca.organization.application;

import io.github.oneofwolvesbilly.orca.organization.domain.Group;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupInvitation;

import java.util.Objects;

/** Executes the member invitation orchestration workflow (Spec 02). */
public final class InviteMemberUseCase {

    private final GroupRepository groupRepository;
    private final RegisteredUserDirectory registeredUserDirectory;

    public InviteMemberUseCase(GroupRepository groupRepository, RegisteredUserDirectory registeredUserDirectory) {
        this.groupRepository = Objects.requireNonNull(groupRepository, "groupRepository");
        this.registeredUserDirectory = Objects.requireNonNull(registeredUserDirectory, "registeredUserDirectory");
    }

    public InviteMemberResult handle(InviteMemberCommand command) {
        Objects.requireNonNull(command, "command");

        if (!registeredUserDirectory.exists(command.inviteeUserId())) {
            throw new IllegalArgumentException("Invitee user does not exist: " + command.inviteeUserId().value());
        }

        Group group = groupRepository.findById(command.groupId())
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + command.groupId().value()));

        GroupInvitation invitation = group.inviteMember(
                command.inviterUserId(),
                command.inviteeUserId(),
                command.intendedRole()
        );

        // Persist the aggregate and its invitation lookup as one repository operation.
        groupRepository.save(group);

        return new InviteMemberResult(invitation.id());
    }
}
