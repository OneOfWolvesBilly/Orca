package io.github.oneofwolvesbilly.orca.organization.infrastructure.inmemory;

import io.github.oneofwolvesbilly.orca.organization.application.GroupRepository;
import io.github.oneofwolvesbilly.orca.organization.domain.Group;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupId;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupInvitationId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Stores groups in memory for tests and local runs. */
public final class InMemoryGroupRepository implements GroupRepository {

    private final Map<GroupId, Group> groupsById = new HashMap<>();
    private final Map<GroupInvitationId, GroupId> invitationToGroupId = new HashMap<>();
    private final List<Group> savedGroups = new ArrayList<>();

    @Override
    public Optional<Group> findById(GroupId groupId) {
        return Optional.ofNullable(groupsById.get(Objects.requireNonNull(groupId, "groupId")));
    }

    @Override
    public Optional<Group> findByInvitationId(GroupInvitationId invitationId) {
        Objects.requireNonNull(invitationId, "invitationId");

        GroupId groupId = invitationToGroupId.get(invitationId);
        if (groupId == null) {
            return Optional.empty();
        }
        return findById(groupId);
    }

    @Override
    public void indexInvitation(GroupInvitationId invitationId, GroupId groupId) {
        invitationToGroupId.put(
                Objects.requireNonNull(invitationId, "invitationId"),
                Objects.requireNonNull(groupId, "groupId")
        );
    }

    @Override
    public void save(Group group) {
        Group nonNull = Objects.requireNonNull(group, "group");
        groupsById.put(nonNull.id(), nonNull);
        invitationToGroupId.entrySet().removeIf(entry -> entry.getValue().equals(nonNull.id()));
        nonNull.invitations().forEach(invitation -> invitationToGroupId.put(invitation.id(), nonNull.id()));
        savedGroups.add(nonNull);
    }

    /** Returns saved groups for assertions. */
    public List<Group> savedGroups() {
        return Collections.unmodifiableList(savedGroups);
    }

    /** Returns invitation indexes for assertions. */
    public Map<GroupInvitationId, GroupId> indexedInvitations() {
        return Collections.unmodifiableMap(invitationToGroupId);
    }
}
