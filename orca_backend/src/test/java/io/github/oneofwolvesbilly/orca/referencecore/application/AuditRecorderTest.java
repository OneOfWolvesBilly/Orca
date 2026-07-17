package io.github.oneofwolvesbilly.orca.referencecore.application;

import io.github.oneofwolvesbilly.orca.referencecore.support.RecordingAuditRecorder;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuditRecorderTest {

    @Test
    void replaceable_recorders_receive_the_same_validated_record() {
        AuditRecord record = record("workflow.action");
        var firstRecorder = new RecordingAuditRecorder();
        var secondRecorder = new RecordingAuditRecorder();

        firstRecorder.record(record);
        secondRecorder.record(record);

        assertEquals(List.of(record), firstRecorder.records());
        assertEquals(List.of(record), secondRecorder.records());
    }

    @Test
    void recorder_failure_remains_observable_to_the_caller() {
        AuditRecorder recorder = record -> {
            throw new IllegalStateException("recorder unavailable");
        };

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> recorder.record(record("workflow.action"))
        );

        assertEquals("recorder unavailable", failure.getMessage());
    }

    @Test
    void consuming_product_maps_its_typed_event_outside_reference_core() {
        ProductAuditEvent event = new ProductAuditEvent("product.action", "actor-1", "approved");

        AuditRecord record = map(event);

        assertEquals("product.action", record.eventType().value());
        assertEquals("actor-1", record.actorId().value());
        assertEquals(List.of(AuditMetadataEntry.of("decision", "approved")), record.metadata().entries());
    }

    private static AuditRecord map(ProductAuditEvent event) {
        return AuditRecord.create(
                AuditEventType.of(event.eventType()),
                AuditActorId.of(event.actorId()),
                Instant.parse("2026-07-16T08:00:00Z"),
                AuditOutcome.of("completed"),
                null,
                null,
                null,
                AuditMetadata.of(List.of(AuditMetadataEntry.of("decision", event.decision())))
        );
    }

    private static AuditRecord record(String eventType) {
        return AuditRecord.create(
                AuditEventType.of(eventType),
                AuditActorId.of("actor-1"),
                Instant.parse("2026-07-16T08:00:00Z"),
                AuditOutcome.of("completed")
        );
    }

    private record ProductAuditEvent(String eventType, String actorId, String decision) {
    }
}
