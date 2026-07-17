package io.github.oneofwolvesbilly.orca.referencecore.application;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuditRecordTest {

    private static final AuditEventType EVENT_TYPE = AuditEventType.of("workflow.action");
    private static final AuditActorId ACTOR_ID = AuditActorId.of("actor-1");
    private static final Instant OCCURRED_AT = Instant.parse("2026-07-16T08:00:00Z");
    private static final AuditOutcome OUTCOME = AuditOutcome.of("completed");

    @Test
    void creates_product_neutral_audit_record() {
        AuditMetadata metadata = AuditMetadata.of(List.of(
                AuditMetadataEntry.of("reason", "approved")
        ));

        AuditRecord record = AuditRecord.create(
                EVENT_TYPE,
                ACTOR_ID,
                OCCURRED_AT,
                OUTCOME,
                "tenant-1",
                "resource",
                "resource-1",
                metadata
        );

        assertEquals(EVENT_TYPE, record.eventType());
        assertEquals(ACTOR_ID, record.actorId());
        assertEquals(OCCURRED_AT, record.occurredAt());
        assertEquals(OUTCOME, record.outcome());
        assertEquals("tenant-1", record.tenantId());
        assertEquals("resource", record.resourceType());
        assertEquals("resource-1", record.resourceId());
        assertEquals(List.of(AuditMetadataEntry.of("reason", "approved")), record.metadata().entries());
    }

    @Test
    void creates_record_without_optional_fields() {
        AuditRecord record = AuditRecord.create(EVENT_TYPE, ACTOR_ID, OCCURRED_AT, OUTCOME);

        assertEquals(AuditMetadata.empty(), record.metadata());
    }

    @Test
    void rejects_missing_or_blank_required_fields() {
        assertThrows(NullPointerException.class, () -> AuditEventType.of(null));
        assertThrows(IllegalArgumentException.class, () -> AuditEventType.of(" "));
        assertThrows(NullPointerException.class, () -> AuditActorId.of(null));
        assertThrows(IllegalArgumentException.class, () -> AuditActorId.of(" "));
        assertThrows(NullPointerException.class, () -> AuditOutcome.of(null));
        assertThrows(IllegalArgumentException.class, () -> AuditOutcome.of(" "));
        assertThrows(NullPointerException.class, () ->
                AuditRecord.create(null, ACTOR_ID, OCCURRED_AT, OUTCOME));
        assertThrows(NullPointerException.class, () ->
                AuditRecord.create(EVENT_TYPE, null, OCCURRED_AT, OUTCOME));
        assertThrows(NullPointerException.class, () ->
                AuditRecord.create(EVENT_TYPE, ACTOR_ID, null, OUTCOME));
        assertThrows(NullPointerException.class, () ->
                AuditRecord.create(EVENT_TYPE, ACTOR_ID, OCCURRED_AT, null));
    }

    @Test
    void rejects_blank_optional_identifiers() {
        assertThrows(IllegalArgumentException.class, () -> AuditRecord.create(
                EVENT_TYPE,
                ACTOR_ID,
                OCCURRED_AT,
                OUTCOME,
                " ",
                null,
                null,
                AuditMetadata.empty()
        ));
        assertThrows(IllegalArgumentException.class, () -> AuditRecord.create(
                EVENT_TYPE,
                ACTOR_ID,
                OCCURRED_AT,
                OUTCOME,
                null,
                " ",
                null,
                AuditMetadata.empty()
        ));
        assertThrows(IllegalArgumentException.class, () -> AuditRecord.create(
                EVENT_TYPE,
                ACTOR_ID,
                OCCURRED_AT,
                OUTCOME,
                null,
                null,
                " ",
                AuditMetadata.empty()
        ));
    }

    @Test
    void metadata_is_immutable_after_creation() {
        var source = new ArrayList<>(List.of(AuditMetadataEntry.of("reason", "approved")));
        AuditMetadata metadata = AuditMetadata.of(source);

        source.clear();

        assertEquals(List.of(AuditMetadataEntry.of("reason", "approved")), metadata.entries());
        assertThrows(UnsupportedOperationException.class, () ->
                metadata.entries().add(AuditMetadataEntry.of("other", "value")));
    }

    @Test
    void rejects_blank_or_duplicate_metadata_entries() {
        assertThrows(NullPointerException.class, () -> AuditMetadataEntry.of(null, "value"));
        assertThrows(IllegalArgumentException.class, () -> AuditMetadataEntry.of(" ", "value"));
        assertThrows(NullPointerException.class, () -> AuditMetadataEntry.of("key", null));
        assertThrows(IllegalArgumentException.class, () -> AuditMetadataEntry.of("key", " "));
        assertThrows(IllegalArgumentException.class, () -> AuditMetadata.of(List.of(
                AuditMetadataEntry.of("reason", "approved"),
                AuditMetadataEntry.of("reason", "replayed")
        )));
    }
}
