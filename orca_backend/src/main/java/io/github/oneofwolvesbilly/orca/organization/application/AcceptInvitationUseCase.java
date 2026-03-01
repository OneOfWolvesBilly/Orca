package io.github.oneofwolvesbilly.orca.organization.application;

import io.github.oneofwolvesbilly.orca.organization.domain.Group;

import java.util.Objects;

/** Executes invitation acceptance orchestration (Spec 03). */
public final class AcceptInvitationUseCase {

    private final GroupRepository groupRepository;

    public AcceptInvitationUseCase(GroupRepository groupRepository) {
        this.groupRepository = Objects.requireNonNull(groupRepository, "groupRepository");
    }

    public void handle(AcceptInvitationCommand command) {
        Objects.requireNonNull(command, "command");

        Group group = groupRepository.findByInvitationId(command.invitationId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Group not found for invitationId=" + command.invitationId().value()
                ));

        group.acceptInvitation(command.invitationId(), command.actorUserId());

        groupRepository.save(group);
    }
}