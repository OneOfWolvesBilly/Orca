package io.github.oneofwolvesbilly.orca.organization.application;

import io.github.oneofwolvesbilly.orca.organization.domain.Group;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupId;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupName;
import io.github.oneofwolvesbilly.orca.organization.domain.UserId;
import io.github.oneofwolvesbilly.orca.organization.infrastructure.inmemory.InMemoryAuditRecorder;
import io.github.oneofwolvesbilly.orca.organization.infrastructure.inmemory.InMemoryGroupRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies application orchestration for group creation. */
class CreateGroupUseCaseTest {

    @Test
    void handle_creates_group_persists_it_and_records_audit_event() {
        var repo = new InMemoryGroupRepository();
        var audit = new InMemoryAuditRecorder();

        GroupIdGenerator idGenerator = () -> GroupId.of("g-100");

        var useCase = new CreateGroupUseCase(repo, audit, idGenerator);

        var result = useCase.handle(new CreateGroupCommand(UserId.of("u-1"), "Team X", "hello"));

        assertEquals("g-100", result.groupId().value());

        assertEquals(1, repo.savedGroups().size());
        var saved = repo.savedGroups().get(0);
        assertEquals("g-100", saved.id().value());
        assertEquals("Team X", saved.name().value());
        assertTrue(saved.description().isPresent());
        assertEquals("hello", saved.description().orElseThrow().value());

        assertEquals(1, audit.recordedEvents().size());
        var event = audit.recordedEvents().get(0);
        assertEquals("GROUP_CREATED", event.type());
        assertEquals("g-100", event.aggregateId());
    }

    @Test
    void spec01_handle_rejects_duplicate_group_id_without_saving_audit_or_changing_existing_group() {
        var repo = new InMemoryGroupRepository();
        var audit = new InMemoryAuditRecorder();

        var groupId = GroupId.of("g-100");
        var creatorId = UserId.of("u-1");
        Group existingGroup = Group.create(groupId, GroupName.of("Existing Team"), null, creatorId);
        repo.save(existingGroup);

        GroupIdGenerator idGenerator = () -> groupId;

        var useCase = new CreateGroupUseCase(repo, audit, idGenerator);

        int savesBeforeFailure = repo.savedGroups().size();
        int membersBeforeFailure = existingGroup.members().size();

        assertThrows(IllegalArgumentException.class, () ->
                useCase.handle(new CreateGroupCommand(UserId.of("u-2"), "Team X", "hello"))
        );

        Group after = repo.findById(groupId).orElseThrow();
        assertEquals(savesBeforeFailure, repo.savedGroups().size());
        assertTrue(audit.recordedEvents().isEmpty());
        assertEquals("Existing Team", after.name().value());
        assertEquals(membersBeforeFailure, after.members().size());
        assertSame(existingGroup, after);
    }
}
