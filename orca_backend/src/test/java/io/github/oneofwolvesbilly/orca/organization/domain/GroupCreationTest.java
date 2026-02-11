package io.github.oneofwolvesbilly.orca.organization.domain;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GroupCreationTest {
    @Test
    void creator_becomes_admin_and_only_member() {
        Group group = Group.create(
                GroupId.of("G-1"),
                GroupName.of("Orca"),
                GroupDescription.ofNullable(null),
                UserId.of("U-1")
        );

        assertEquals(1, group.members().size());
        assertEquals(GroupRole.GROUP_ADMIN, group.members().getFirst().role());
    }
}
