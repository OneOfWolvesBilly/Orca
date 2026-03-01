package io.github.oneofwolvesbilly.orca.organization.application;

import io.github.oneofwolvesbilly.orca.organization.domain.Group;

import java.util.Objects;

/** Executes invitation rejection orchestration (Spec 04). */
public final class RejectInvitationUseCase {

    private final GroupRepository groupRepository;

    public RejectInvitationUseCase(GroupRepository groupRepository) {
        this.groupRepository = Objects.requireNonNull(groupRepository, "groupRepository");
    }

    public void handle(RejectInvitationCommand command) {
        Objects.requireNonNull(command, "command");

        Group group = groupRepository.findByInvitationId(command.invitationId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Group not found for invitationId=" + command.invitationId().value()
                ));

        group.rejectInvitation(command.invitationId(), command.actorUserId());

        groupRepository.save(group);
    }
}