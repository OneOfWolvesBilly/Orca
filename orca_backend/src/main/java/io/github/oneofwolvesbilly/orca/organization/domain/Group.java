package io.github.oneofwolvesbilly.orca.organization.domain;

import java.util.LinkedList;
import java.util.List;

public final class Group {

    private final LinkedList<GroupMember> members;

    private Group(LinkedList<GroupMember> members) {
        this.members = members;
    }

    public static Group create(
            GroupId id,
            GroupName name,
            GroupDescription description,
            UserId creator
    ) {
        LinkedList<GroupMember> members = new LinkedList<>();
        members.add(GroupMember.of(creator, GroupRole.GROUP_ADMIN));
        return new Group(members);
    }

    public List<GroupMember> members() {
        return List.copyOf(members);
    }
}
