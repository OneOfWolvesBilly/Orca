package io.github.oneofwolvesbilly.orca.organization.application;

import io.github.oneofwolvesbilly.orca.organization.domain.Group;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupId;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupInvitationId;

import java.util.Optional;

/** Persists and retrieves Group aggregates. */
public interface GroupRepository {

    Optional<Group> findById(GroupId groupId);

    Optional<Group> findByInvitationId(GroupInvitationId invitationId);

    /**
     * Records a stable mapping from invitationId to owning groupId.
     *
     * <p>This remains available for adapters that manage the lookup separately, but repository {@link #save(Group)}
     * must persist aggregate state and invitation lookup changes as one atomic application operation.
     */
    void indexInvitation(GroupInvitationId invitationId, GroupId groupId);

    void save(Group group);
}
