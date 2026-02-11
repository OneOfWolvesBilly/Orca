package io.github.oneofwolvesbilly.orca.organization.application;

import io.github.oneofwolvesbilly.orca.organization.domain.Group;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupDescription;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupId;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupName;

public final class CreateGroupUseCase {

    private final GroupRepository groupRepository;
    private final AuditRecorder auditRecorder;
    private final GroupIdGenerator idGenerator;

    public CreateGroupUseCase(
            GroupRepository groupRepository,
            AuditRecorder auditRecorder,
            GroupIdGenerator idGenerator
    ) {
        this.groupRepository = groupRepository;
        this.auditRecorder = auditRecorder;
        this.idGenerator = idGenerator;
    }

    public CreateGroupResult handle(CreateGroupCommand command) {
        GroupId groupId = idGenerator.nextId();

        Group group = Group.create(
                groupId,
                GroupName.of(command.name()),
                GroupDescription.ofNullable(command.description()),
                command.creatorUserId()
        );

        groupRepository.save(group);
        auditRecorder.record(AuditEvent.groupCreated(groupId));

        return new CreateGroupResult(groupId);
    }
}
