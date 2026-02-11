package io.github.oneofwolvesbilly.orca.organization.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Holds group state and enforces membership invariants. */
public final class Group {

    private final GroupId id;
    private final GroupName name;
    private final GroupDescription description; // nullable
    private final List<GroupMember> members;

    private Group(GroupId id, GroupName name, GroupDescription description, List<GroupMember> members) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.description = description;
        this.members = new ArrayList<>(Objects.requireNonNull(members, "members"));
        validateInvariants();
    }

    /** Creates a group with the creator as the initial admin member. */
    public static Group create(GroupId id, GroupName name, GroupDescription description, UserId creator) {
        Objects.requireNonNull(creator, "creator");

        var members = new ArrayList<GroupMember>();
        members.add(GroupMember.of(creator, GroupRole.GROUP_ADMIN));

        return new Group(id, name, description, members);
    }

    public GroupId id() {
        return id;
    }

    public GroupName name() {
        return name;
    }

    /** Exposes description as Optional to avoid leaking null. */
    public Optional<GroupDescription> description() {
        return Optional.ofNullable(description);
    }

    public List<GroupMember> members() {
        return Collections.unmodifiableList(members);
    }

    /** Validates invariant rules for group membership. */
    private void validateInvariants() {
        if (members.isEmpty()) {
            throw new IllegalStateException("group must have at least one member");
        }

        boolean hasAdmin = members.stream().anyMatch(m -> m.role() == GroupRole.GROUP_ADMIN);
        if (!hasAdmin) {
            throw new IllegalStateException("group must have at least one admin");
        }

        long distinct = members.stream().map(m -> m.userId().value()).distinct().count();
        if (distinct != members.size()) {
            throw new IllegalStateException("duplicate membership is not allowed");
        }
    }
}