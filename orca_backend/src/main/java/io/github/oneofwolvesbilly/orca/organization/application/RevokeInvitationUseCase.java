package io.github.oneofwolvesbilly.orca.organization.application;

import io.github.oneofwolvesbilly.orca.organization.domain.Group;

import java.util.Objects;

/** Executes invitation revocation orchestration (Spec 05). */
public final class RevokeInvitationUseCase {

    private final GroupRepository groupRepository;

    public RevokeInvitationUseCase(GroupRepository groupRepository) {
        this.groupRepository = Objects.requireNonNull(groupRepository, "groupRepository");
    }

    public void handle(RevokeInvitationCommand command) {
        Objects.requireNonNull(command, "command");

        Group group = groupRepository.findByInvitationId(command.invitationId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Group not found for invitationId=" + command.invitationId().value()
                ));

        group.revokeInvitation(command.invitationId(), command.actorUserId());

        groupRepository.save(group);
    }
}