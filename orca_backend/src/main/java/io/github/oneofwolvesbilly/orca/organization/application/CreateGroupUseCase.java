package io.github.oneofwolvesbilly.orca.organization.application;

import io.github.oneofwolvesbilly.orca.organization.domain.Group;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupDescription;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupId;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupName;

import java.util.Objects;

/** Executes the group creation workflow. */
public final class CreateGroupUseCase {

    private final GroupRepository groupRepository;
    private final AuditRecorder auditRecorder;
    private final GroupIdGenerator idGenerator;

    public CreateGroupUseCase(
            GroupRepository groupRepository,
            AuditRecorder auditRecorder,
            GroupIdGenerator idGenerator
    ) {
        this.groupRepository = Objects.requireNonNull(groupRepository, "groupRepository");
        this.auditRecorder = Objects.requireNonNull(auditRecorder, "auditRecorder");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    /** Creates a group, persists it, and records an audit event. */
    public CreateGroupResult handle(CreateGroupCommand command) {
        Objects.requireNonNull(command, "command");

        GroupId groupId = Objects.requireNonNull(idGenerator.nextId(), "groupId");

        GroupDescription description = command.description() == null
                ? null
                : GroupDescription.of(command.description());

        Group group = Group.create(
                groupId,
                GroupName.of(command.name()),
                description,
                command.creatorUserId()
        );

        groupRepository.save(group);
        auditRecorder.record(AuditEvent.groupCreated(groupId));

        return CreateGroupResult.created(groupId);
    }
}