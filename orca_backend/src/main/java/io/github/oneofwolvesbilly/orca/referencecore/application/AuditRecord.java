package io.github.oneofwolvesbilly.orca.referencecore.application;

import java.time.Instant;
import java.util.Objects;

public record AuditRecord(
        AuditEventType eventType,
        AuditActorId actorId,
        Instant occurredAt,
        AuditOutcome outcome,
        String tenantId,
        String resourceType,
        String resourceId,
        AuditMetadata metadata
) {

    public AuditRecord {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(metadata, "metadata");
        requireNonBlankWhenPresent(tenantId, "tenantId");
        requireNonBlankWhenPresent(resourceType, "resourceType");
        requireNonBlankWhenPresent(resourceId, "resourceId");
    }

    public static AuditRecord create(
            AuditEventType eventType,
            AuditActorId actorId,
            Instant occurredAt,
            AuditOutcome outcome
    ) {
        return create(
                eventType,
                actorId,
                occurredAt,
                outcome,
                null,
                null,
                null,
                AuditMetadata.empty()
        );
    }

    public static AuditRecord create(
            AuditEventType eventType,
            AuditActorId actorId,
            Instant occurredAt,
            AuditOutcome outcome,
            String tenantId,
            String resourceType,
            String resourceId,
            AuditMetadata metadata
    ) {
        return new AuditRecord(
                eventType,
                actorId,
                occurredAt,
                outcome,
                tenantId,
                resourceType,
                resourceId,
                metadata
        );
    }

    private static void requireNonBlankWhenPresent(String value, String fieldName) {
        if (value != null && value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank when present");
        }
    }
}
