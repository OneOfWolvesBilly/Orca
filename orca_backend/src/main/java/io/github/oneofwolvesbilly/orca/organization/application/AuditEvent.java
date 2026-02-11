package io.github.oneofwolvesbilly.orca.organization.application;

import io.github.oneofwolvesbilly.orca.organization.domain.GroupId;

public record AuditEvent(String type, String aggregateId) {
    public static AuditEvent groupCreated(GroupId groupId) {
        return new AuditEvent("GROUP_CREATED", groupId.value());
    }
}
