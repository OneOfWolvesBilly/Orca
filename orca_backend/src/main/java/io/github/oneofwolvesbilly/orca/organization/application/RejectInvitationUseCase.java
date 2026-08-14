package io.github.oneofwolvesbilly.orca.organization.application;

import io.github.oneofwolvesbilly.orca.organization.domain.Group;
import io.github.oneofwolvesbilly.orca.organization.domain.DomainException;

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
                .orElseThrow(() -> OrganizationFailures.notFound(
                        "Invitation lookup did not resolve a group: " + command.invitationId().value()
                ));

        try {
            group.rejectInvitation(command.invitationId(), command.actorUserId());
        } catch (DomainException failure) {
            throw OrganizationFailures.from(failure);
        }

        groupRepository.save(group);
    }
}
