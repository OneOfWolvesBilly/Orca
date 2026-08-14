package io.github.oneofwolvesbilly.orca.organization.application;

import io.github.oneofwolvesbilly.orca.organization.domain.Group;
import io.github.oneofwolvesbilly.orca.organization.domain.DomainException;

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
                .orElseThrow(() -> OrganizationFailures.notFound(
                        "Invitation lookup did not resolve a group: " + command.invitationId().value()
                ));

        try {
            group.revokeInvitation(command.invitationId(), command.actorUserId());
        } catch (DomainException failure) {
            throw OrganizationFailures.from(failure);
        }

        groupRepository.save(group);
    }
}
