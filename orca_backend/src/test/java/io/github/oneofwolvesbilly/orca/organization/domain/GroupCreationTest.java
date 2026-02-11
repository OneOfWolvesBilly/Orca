package io.github.oneofwolvesbilly.orca.organization.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies group creation invariants and initial state. */
class GroupCreationTest {

    @Test
    void create_sets_identity_attributes_and_creator_as_admin_member() {
        var group = Group.create(
                GroupId.of("g-1"),
                GroupName.of("Team A"),
                GroupDescription.of("desc"),
                UserId.of("u-1")
        );

        assertEquals("g-1", group.id().value());
        assertEquals("Team A", group.name().value());
        assertTrue(group.description().isPresent());
        assertEquals("desc", group.description().orElseThrow().value());

        assertEquals(1, group.members().size());
        assertEquals("u-1", group.members().get(0).userId().value());
        assertEquals(GroupRole.GROUP_ADMIN, group.members().get(0).role());
    }

    @Test
    void create_allows_null_description_as_absent_optional() {
        var group = Group.create(
                GroupId.of("g-2"),
                GroupName.of("Team B"),
                null,
                UserId.of("u-2")
        );

        assertTrue(group.description().isEmpty());
    }
}
