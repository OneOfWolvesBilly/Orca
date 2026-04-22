package io.github.oneofwolvesbilly.orca.organization.infrastructure.persistence;

import io.github.oneofwolvesbilly.orca.organization.domain.Group;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupDescription;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupId;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupInvitation;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupInvitationId;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupMember;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupName;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupRole;
import io.github.oneofwolvesbilly.orca.organization.domain.InvitationStatus;
import io.github.oneofwolvesbilly.orca.organization.domain.UserId;

import java.util.List;
import java.util.Objects;

public final class GroupEntityMapper {

    GroupRecord toGroupRecord(Group group) {
        Objects.requireNonNull(group, "group");
        return new GroupRecord(
                group.id().value(),
                group.name().value(),
                group.description().map(GroupDescription::value).orElse(null)
        );
    }

    List<MemberRecord> toMemberRecords(Group group) {
        Objects.requireNonNull(group, "group");
        return group.members().stream()
                .map(member -> new MemberRecord(
                        group.id().value(),
                        member.userId().value(),
                        member.role().name()
                ))
                .toList();
    }

    List<InvitationRecord> toInvitationRecords(Group group) {
        Objects.requireNonNull(group, "group");
        return group.invitations().stream()
                .map(invitation -> new InvitationRecord(
                        invitation.id().value(),
                        invitation.groupId().value(),
                        invitation.inviteeUserId().value(),
                        invitation.intendedRole().name(),
                        invitation.status().name()
                ))
                .toList();
    }

    Group toDomain(GroupRecord group, List<MemberRecord> members, List<InvitationRecord> invitations) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(members, "members");
        Objects.requireNonNull(invitations, "invitations");

        return Group.reconstitute(
                GroupId.of(group.id()),
                GroupName.of(group.name()),
                group.description() == null ? null : GroupDescription.of(group.description()),
                members.stream()
                        .map(member -> GroupMember.of(
                                UserId.of(member.userId()),
                                GroupRole.valueOf(member.role())
                        ))
                        .toList(),
                invitations.stream()
                        .map(invitation -> new GroupInvitation(
                                GroupInvitationId.of(invitation.id()),
                                GroupId.of(invitation.groupId()),
                                UserId.of(invitation.inviteeId()),
                                GroupRole.valueOf(invitation.intendedRole()),
                                InvitationStatus.valueOf(invitation.status())
                        ))
                        .toList()
        );
    }

    record GroupRecord(String id, String name, String description) {
    }

    record MemberRecord(String groupId, String userId, String role) {
    }

    record InvitationRecord(String id, String groupId, String inviteeId, String intendedRole, String status) {
    }
}
