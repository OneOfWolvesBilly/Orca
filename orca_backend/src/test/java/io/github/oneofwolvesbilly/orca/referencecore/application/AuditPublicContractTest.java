package io.github.oneofwolvesbilly.orca.referencecore.application;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import static org.junit.jupiter.api.Assertions.*;

class AuditPublicContractTest {
    private static final AuditEventType EVENT = AuditEventType.of("custom.event");
    private static final AuditActorId ACTOR = AuditActorId.of("actor");
    private static final Instant TIME = Instant.parse("2000-01-01T00:00:00Z");
    private static final AuditOutcome OUTCOME = AuditOutcome.of("custom.outcome");

    @Test
    void constructors_and_factories_reject_missing_and_blank_identifiers() {
        List<Function<String, ?>> paths = List.of(AuditEventType::new, AuditEventType::of,
                AuditActorId::new, AuditActorId::of, AuditOutcome::new, AuditOutcome::of);
        for (var path : paths) {
            assertThrows(NullPointerException.class, () -> path.apply(null));
            for (String blank : List.of("", " ", "\t\n")) {
                assertThrows(IllegalArgumentException.class, () -> path.apply(blank));
            }
            assertNotNull(path.apply("consumer-defined"));
        }
    }

    @Test
    void full_creation_paths_reject_invalid_fields_before_recording() {
        for (boolean constructor : List.of(true, false)) {
            for (int position : new int[]{0, 1, 2, 3, 7}) {
                Object[] values = values();
                values[position] = null;
                rejectsBeforeRecording(NullPointerException.class, () -> full(constructor, values));
            }
            for (int position : new int[]{4, 5, 6}) {
                for (String blank : List.of("", " ", "\t\n")) {
                    Object[] values = values();
                    values[position] = blank;
                    rejectsBeforeRecording(IllegalArgumentException.class, () -> full(constructor, values));
                }
            }
        }
        for (int position : new int[]{0, 1, 2, 3}) {
            Object[] v = values();
            v[position] = null;
            rejectsBeforeRecording(NullPointerException.class, () -> AuditRecord.create(
                    (AuditEventType) v[0], (AuditActorId) v[1], (Instant) v[2], (AuditOutcome) v[3]));
        }
    }

    @Test
    void metadata_paths_reject_invalid_entries_and_duplicate_keys() {
        List<BiFunction<String, String, AuditMetadataEntry>> entries = List.of(
                AuditMetadataEntry::new, AuditMetadataEntry::of);
        for (var entry : entries) {
            assertThrows(NullPointerException.class, () -> entry.apply(null, "value"));
            assertThrows(NullPointerException.class, () -> entry.apply("key", null));
            for (String blank : List.of("", " ", "\t\n")) {
                assertThrows(IllegalArgumentException.class, () -> entry.apply(blank, "value"));
                assertThrows(IllegalArgumentException.class, () -> entry.apply("key", blank));
            }
        }
        for (boolean constructor : List.of(true, false)) {
            rejectsBeforeRecording(NullPointerException.class, () -> withMetadata(metadata(constructor, null)));
            rejectsBeforeRecording(NullPointerException.class, () -> withMetadata(
                    metadata(constructor, Arrays.asList((AuditMetadataEntry) null))));
            for (String secondValue : List.of("value", "different")) {
                rejectsBeforeRecording(IllegalArgumentException.class, () -> withMetadata(metadata(constructor,
                        List.of(AuditMetadataEntry.of("key", "value"), AuditMetadataEntry.of("key", secondValue)))));
            }
        }
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void erased_collection_misuse_is_rejected_before_recording() {
        for (boolean constructor : List.of(true, false)) {
            for (Object invalid : List.of("text", new byte[]{1}, List.of("nested"),
                    Map.of("key", "value"), new Exception("synthetic"), new Object())) {
                List raw = List.of(invalid);
                rejectsBeforeRecording(RuntimeException.class, () -> withMetadata(metadata(constructor, raw)));
            }
        }
    }

    @Test
    void defaults_value_equality_and_defensive_copy_are_preserved() {
        AuditRecord minimal = AuditRecord.create(EVENT, ACTOR, TIME, OUTCOME);
        assertNull(minimal.tenantId());
        assertNull(minimal.resourceType());
        assertNull(minimal.resourceId());
        assertEquals(TIME, minimal.occurredAt());
        assertEquals(AuditMetadata.empty(), minimal.metadata());
        assertEquals(new AuditEventType("custom.event"), EVENT);
        assertEquals(new AuditActorId("actor"), ACTOR);
        assertEquals(new AuditOutcome("custom.outcome"), OUTCOME);
        for (boolean constructor : List.of(true, false)) {
            var input = new ArrayList<>(List.of(new AuditMetadataEntry("key", "value")));
            AuditMetadata metadata = metadata(constructor, input);
            input.clear();
            assertEquals(List.of(AuditMetadataEntry.of("key", "value")), metadata.entries());
            assertThrows(UnsupportedOperationException.class, () -> metadata.entries().clear());
            assertThrows(UnsupportedOperationException.class, () -> metadata.entries().set(0, new AuditMetadataEntry("x", "y")));
            AuditRecord record = withMetadata(metadata);
            AuditRecord equal = new AuditRecord(EVENT, ACTOR, TIME, OUTCOME, null, null, null,
                    new AuditMetadata(List.of(new AuditMetadataEntry("key", "value"))));
            assertEquals(equal, record);
            assertEquals(equal.hashCode(), record.hashCode());
        }
    }

    @Test
    void recorder_has_no_implicit_deduplication_or_failure_policy() {
        AuditRecord record = AuditRecord.create(EVENT, ACTOR, TIME, OUTCOME);
        var captured = new ArrayList<AuditRecord>();
        AuditRecorder recorder = captured::add;
        recorder.record(record);
        recorder.record(record);
        assertEquals(List.of(record, record), captured);
        var calls = new AtomicInteger();
        var failure = new IllegalStateException("synthetic recorder failure");
        AuditRecorder failing = value -> { calls.incrementAndGet(); throw failure; };
        assertSame(failure, assertThrows(IllegalStateException.class, () -> failing.record(record)));
        assertEquals(1, calls.get());
    }

    private static Object[] values() {
        return new Object[]{EVENT, ACTOR, TIME, OUTCOME, null, null, null, AuditMetadata.empty()};
    }

    private static AuditRecord full(boolean constructor, Object[] v) {
        return constructor
                ? new AuditRecord((AuditEventType) v[0], (AuditActorId) v[1], (Instant) v[2],
                    (AuditOutcome) v[3], (String) v[4], (String) v[5], (String) v[6], (AuditMetadata) v[7])
                : AuditRecord.create((AuditEventType) v[0], (AuditActorId) v[1], (Instant) v[2],
                    (AuditOutcome) v[3], (String) v[4], (String) v[5], (String) v[6], (AuditMetadata) v[7]);
    }

    private static AuditMetadata metadata(boolean constructor, List<AuditMetadataEntry> entries) {
        return constructor ? new AuditMetadata(entries) : AuditMetadata.of(entries);
    }

    private static AuditRecord withMetadata(AuditMetadata metadata) {
        return AuditRecord.create(EVENT, ACTOR, TIME, OUTCOME, null, null, null, metadata);
    }

    private static <T extends Throwable> void rejectsBeforeRecording(Class<T> failure, Supplier<AuditRecord> create) {
        var calls = new AtomicInteger();
        AuditRecorder recorder = record -> calls.incrementAndGet();
        assertThrows(failure, () -> recorder.record(create.get()));
        assertEquals(0, calls.get());
    }
}
