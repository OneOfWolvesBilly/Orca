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

    // Spec 03: Stable lookup by invitation id (needed after acceptance).
    private final Map<GroupInvitationId, GroupInvitation> invitationsById;

    private Group(GroupId id, GroupName name, GroupDescription description, List<GroupMember> members) {
        this(id, name, description, members, new HashMap<>(), new HashMap<>());
    }

    private Group(GroupId id,
                  GroupName name,
                  GroupDescription description,
                  List<GroupMember> members,
                  Map<UserId, GroupInvitation> pendingInvitations) {
        this(id, name, description, members,pendingInvitations ,new HashMap<>());
    }

    private Group(GroupId id,
                  GroupName name,
                  GroupDescription description,
                  List<GroupMember> members,
                  Map<UserId, GroupInvitation> pendingInvitations,
                  Map<GroupInvitationId, GroupInvitation> invitationsById) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.description = description;
        this.members = new ArrayList<>(Objects.requireNonNull(members, "members"));
        this.pendingInvitations = new HashMap<>(Objects.requireNonNull(pendingInvitations, "pendingInvitations"));
        this.invitationsById = new HashMap<>(Objects.requireNonNull(invitationsById, "invitationsById"));
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
        ensureInviterIsGroupAdmin(inviterUserId, "Only GROUP_ADMIN can invite members.");

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
        invitationsById.put(invitation.id(), invitation);
        return invitation;
    }

    public boolean hasPendingInvitationFor(UserId inviteeUserId) {
        Objects.requireNonNull(inviteeUserId, "inviteeUserId");
        return pendingInvitations.containsKey(inviteeUserId);
    }

    boolean isMember(UserId userId) {
        return members.stream().anyMatch(m -> m.userId().equals(userId));
    }
    
    private void ensureInviterIsGroupAdmin(UserId userId, String message) {
        GroupMember inviter = members.stream()
                .filter(m -> m.userId().equals(userId))
                .findFirst()
                .orElse(null);

        if (inviter == null || inviter.role() != GroupRole.GROUP_ADMIN) {
            throw new DomainException(DomainError.INVITER_NOT_GROUP_ADMIN, message);
        }
    }

    void acceptInvitation(GroupInvitationId invitationId, UserId acceptingUserId) {
        Objects.requireNonNull(invitationId, "invitationId");
        Objects.requireNonNull(acceptingUserId, "acceptingUserId");

        GroupInvitation inv = invitationsById.get(invitationId);
        if (inv == null) {
            throw new DomainException(DomainError.INVITATION_NOT_FOUND, "Invitation not found.");
        }

        if (inv.status() != InvitationStatus.PENDING) {
            throw new DomainException(DomainError.INVITATION_NOT_PENDING, "Only PENDING invitation can be accepted.");
        }

        if (!inv.inviteeUserId().equals(acceptingUserId)) {
            throw new DomainException(DomainError.INVITATION_ACCEPTOR_MISMATCH, "Only invitee can accept.");
        }

        if (isMember(acceptingUserId)) {
            throw new DomainException(DomainError.INVITEE_ALREADY_MEMBER, "Invitee is already a group member.");
        }

        // Atomic in-memory: membership + invitation accepted + remove pending link
        addMember(acceptingUserId, inv.intendedRole());

        GroupInvitation accepted = new GroupInvitation(
                inv.id(),
                inv.groupId(),
                inv.inviteeUserId(),
                inv.intendedRole(),
                InvitationStatus.ACCEPTED
        );

        invitationsById.put(invitationId, accepted);
        pendingInvitations.remove(inv.inviteeUserId());

        validateInvariants();
    }

    void rejectInvitation(GroupInvitationId invitationId, UserId rejectingUserId) {
        Objects.requireNonNull(invitationId, "invitationId");
        Objects.requireNonNull(rejectingUserId, "rejectingUserId");

        GroupInvitation inv = invitationsById.get(invitationId);
        if (inv == null) {
            throw new DomainException(DomainError.INVITATION_NOT_FOUND, "Invitation not found.");
        }

        if (inv.status() != InvitationStatus.PENDING) {
            throw new DomainException(DomainError.INVITATION_NOT_PENDING, "Only PENDING invitation can be rejected.");
        }

        if (!inv.inviteeUserId().equals(rejectingUserId)) {
            throw new DomainException(DomainError.INVITATION_REJECTOR_MISMATCH, "Only invitee can reject.");
        }

        if (isMember(rejectingUserId)) {
            throw new DomainException(DomainError.INVITEE_ALREADY_MEMBER, "Invitee is already a group member.");
        }

        GroupInvitation rejected = new GroupInvitation(
                inv.id(),
                inv.groupId(),
                inv.inviteeUserId(),
                inv.intendedRole(),
                InvitationStatus.REJECTED
        );

        invitationsById.put(invitationId, rejected);
        pendingInvitations.remove(inv.inviteeUserId());

        validateInvariants();
    }
    void revokeInvitation(GroupInvitationId invitationId, UserId revokerUserId) {
        Objects.requireNonNull(invitationId, "invitationId");
        Objects.requireNonNull(revokerUserId, "revokerUserId");

        GroupInvitation inv = invitationsById.get(invitationId);
        if (inv == null) {
            throw new DomainException(DomainError.INVITATION_NOT_FOUND, "Invitation not found.");
        }

        if (inv.status() != InvitationStatus.PENDING) {
            throw new DomainException(DomainError.INVITATION_NOT_PENDING, "Only PENDING invitation can be revoked.");
        }

        // Reuse existing admin check (same rule, different action)
        ensureInviterIsGroupAdmin(revokerUserId,  "Only GROUP_ADMIN can revoke invitations.");

        GroupInvitation revoked = new GroupInvitation(
                inv.id(),
                inv.groupId(),
                inv.inviteeUserId(),
                inv.intendedRole(),
                InvitationStatus.REVOKED
        );

        invitationsById.put(invitationId, revoked);
        pendingInvitations.remove(inv.inviteeUserId());

        validateInvariants();
    }

    GroupInvitation getInvitation(GroupInvitationId invitationId) {
        Objects.requireNonNull(invitationId, "invitationId");
        GroupInvitation inv = invitationsById.get(invitationId);
        if (inv == null) {
            throw new DomainException(DomainError.INVITATION_NOT_FOUND, "Invitation not found.");
        }
        return inv;
    }

    GroupMember getMember(UserId userId) {
        Objects.requireNonNull(userId, "userId");
        return members.stream()
                .filter(m -> m.userId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("member not found: " + userId.value()));
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