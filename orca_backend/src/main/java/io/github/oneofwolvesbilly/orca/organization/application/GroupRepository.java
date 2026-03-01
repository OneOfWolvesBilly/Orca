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
     * <p>This is required for application-layer orchestration where requests carry only an invitationId
     * (Spec 03–05). Adapters that can resolve invitationId natively may treat this as a no-op.
     */
    void indexInvitation(GroupInvitationId invitationId, GroupId groupId);

    void save(Group group);
}