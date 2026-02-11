package io.github.oneofwolvesbilly.orca.organization.application;

import io.github.oneofwolvesbilly.orca.organization.domain.GroupId;

import java.util.Objects;

/** Captures an audit record produced by application workflows. */
public record AuditEvent(String type, String aggregateId) {

    public AuditEvent {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(aggregateId, "aggregateId");
        if (type.isBlank()) throw new IllegalArgumentException("type must not be blank");
        if (aggregateId.isBlank()) throw new IllegalArgumentException("aggregateId must not be blank");
    }

    /** Creates an audit event for group creation. */
    public static AuditEvent groupCreated(GroupId groupId) {
        Objects.requireNonNull(groupId, "groupId");
        return new AuditEvent("GROUP_CREATED", groupId.value());
    }
}
