package io.github.oneofwolvesbilly.orca.organization.application;

import io.github.oneofwolvesbilly.orca.organization.domain.GroupId;
import io.github.oneofwolvesbilly.orca.organization.domain.UserId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CreateGroupUseCaseTest {

    @Test
    void creates_group_and_records_event() {
        InMemoryGroupRepository repo = new InMemoryGroupRepository();
        InMemoryAuditRecorder audit = new InMemoryAuditRecorder();

        CreateGroupUseCase useCase =
                new CreateGroupUseCase(repo, audit, () -> GroupId.of("G-1"));

        CreateGroupResult result = useCase.handle(
                new CreateGroupCommand(UserId.of("U-1"), "Orca", null)
        );

        assertEquals("G-1", result.groupId().value());
        assertEquals(1, audit.events().size());
    }
}
