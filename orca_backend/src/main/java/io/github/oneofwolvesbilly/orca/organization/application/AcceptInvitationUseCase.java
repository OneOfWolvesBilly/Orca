package io.github.oneofwolvesbilly.orca.organization.application;

import io.github.oneofwolvesbilly.orca.organization.domain.Group;
import io.github.oneofwolvesbilly.orca.organization.domain.DomainException;

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
                .orElseThrow(() -> OrganizationFailures.notFound(
                        "Invitation lookup did not resolve a group: " + command.invitationId().value()
                ));

        try {
            group.acceptInvitation(command.invitationId(), command.actorUserId());
        } catch (DomainException failure) {
            throw OrganizationFailures.from(failure);
        }

        groupRepository.save(group);
    }
}
