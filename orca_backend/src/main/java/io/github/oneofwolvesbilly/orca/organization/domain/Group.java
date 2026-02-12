package io.github.oneofwolvesbilly.orca.organization.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Holds group state and enforces membership invariants. */
public final class Group {

    private final GroupId id;
    private final GroupName name;
    private final GroupDescription description; // nullable
    private final List<GroupMember> members;

    // Spec 02: Tracks pending invitations by invitee userId.
    private final Map<UserId, GroupInvitation> pendingInvitations;

    private Group(GroupId id, GroupName name, GroupDescription description, List<GroupMember> members) {
        this(id, name, description, members, new HashMap<>());
    }

    private Group(GroupId id,
                  GroupName name,
                  GroupDescription description,
                  List<GroupMember> members,
                  Map<UserId, GroupInvitation> pendingInvitations) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.description = description;
        this.members = new ArrayList<>(Objects.requireNonNull(members, "members"));
        this.pendingInvitations = new HashMap<>(Objects.requireNonNull(pendingInvitations, "pendingInvitations"));
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

    // ---- Spec 02 - Invite Member ----

    /**
     * Spec 02 - Invite Member
     *
     * Domain invariants enforced:
     * - AC2: Only GROUP_ADMIN can invite.
     * - AC4: Cannot invite an existing member.
     * - AC3: No duplicate PENDING invitation for the same invitee userId.
     *
     * Note:
     * - AC1 (invitee userId must be non-empty) is enforced by UserId value object.
     */
    public GroupInvitation inviteMember(UserId inviterUserId, UserId inviteeUserId, GroupRole intendedRole) {
        Objects.requireNonNull(inviterUserId, "inviterUserId");
        Objects.requireNonNull(inviteeUserId, "inviteeUserId");
        Objects.requireNonNull(intendedRole, "intendedRole");

        // Spec 02 - AC2
        ensureInviterIsGroupAdmin(inviterUserId);

        // Spec 02 - AC4
        if (isMember(inviteeUserId)) {
            throw new DomainException(DomainError.INVITEE_ALREADY_MEMBER, "Invitee is already a group member.");
        }

        // Spec 02 - AC3
        if (pendingInvitations.containsKey(inviteeUserId)) {
            throw new DomainException(DomainError.DUPLICATE_PENDING_INVITATION, "Pending invitation already exists.");
        }

        // Spec 02 - Then: create invitation in PENDING status associated with the group.
        GroupInvitation invitation = new GroupInvitation(
                GroupInvitationId.newId(),
                this.id,
                inviteeUserId,
                intendedRole,
                InvitationStatus.PENDING
        );

        pendingInvitations.put(inviteeUserId, invitation);
        return invitation;
    }

    public boolean hasPendingInvitationFor(UserId inviteeUserId) {
        Objects.requireNonNull(inviteeUserId, "inviteeUserId");
        return pendingInvitations.containsKey(inviteeUserId);
    }

    private boolean isMember(UserId userId) {
        return members.stream().anyMatch(m -> m.userId().equals(userId));
    }

    private void ensureInviterIsGroupAdmin(UserId inviterUserId) {
        GroupMember inviter = members.stream()
                .filter(m -> m.userId().equals(inviterUserId))
                .findFirst()
                .orElse(null);

        if (inviter == null || inviter.role() != GroupRole.GROUP_ADMIN) {
            throw new DomainException(DomainError.INVITER_NOT_GROUP_ADMIN, "Only GROUP_ADMIN can invite members.");
        }
    }

    // package-private is recommended to prevent misuse by application layer.
    // If your tests are in the same package, they can still call it.
    // Change to `public` only if you have a strong reason.
    void addMember(UserId userId, GroupRole role) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(role, "role");

        if (members.stream().anyMatch(m -> m.userId().equals(userId))) {
            throw new IllegalStateException("duplicate membership is not allowed");
        }

        members.add(GroupMember.of(userId, role));
        validateInvariants();
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