package io.github.oneofwolvesbilly.orcaconsumer;

import io.github.oneofwolvesbilly.orca.referencecore.application.AuditActorId;
import io.github.oneofwolvesbilly.orca.referencecore.application.AuditEventType;
import io.github.oneofwolvesbilly.orca.referencecore.application.AuditMetadata;
import io.github.oneofwolvesbilly.orca.referencecore.application.AuditMetadataEntry;
import io.github.oneofwolvesbilly.orca.referencecore.application.AuditOutcome;
import io.github.oneofwolvesbilly.orca.referencecore.application.AuditRecord;
import io.github.oneofwolvesbilly.orca.referencecore.application.AuditRecorder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class AuditArtifactConsumerTest {
    private static final Instant TIME = Instant.parse("2000-01-01T00:00:00Z");
    private final ApplicationContextRunner context = new ApplicationContextRunner().withUserConfiguration(ConsumerConfig.class);

    record FixtureInput(String actor, Instant occurredAt, String confidentialDetail) {}
    static class Caller {
        private final AuditRecorder recorder;
        Caller(AuditRecorder recorder) { this.recorder = recorder; }
        void record(FixtureInput input) {
            recorder.record(AuditRecord.create(AuditEventType.of("fixture.audit-recording"),
                    AuditActorId.of(input.actor()), input.occurredAt(), AuditOutcome.of("completed"),
                    null, null, null, AuditMetadata.of(List.of(AuditMetadataEntry.of("source", "standalone-fixture")))));
        }
    }
    @TestConfiguration(proxyBeanMethods = false)
    static class ConsumerConfig {
        @Bean Caller caller(AuditRecorder recorder) { return new Caller(recorder); }
    }

    @Test
    void spring_consumer_explicitly_wires_replaceable_recorders_and_exact_safe_mapping() {
        for (String forbidden : List.of("password", "credential-secret", "recovery-code", "private-key",
                "authentication-token", "raw-session-cookie", "raw-session-id", "totp-secret",
                "raw-request", "raw-response", "request-headers", "exception-object", "stack-trace", "user-object",
                "credential-object", "session-object", "role-object", "organization-object", "profile-object")) {
            for (boolean alternative : List.of(false, true)) {
                var received = new ArrayList<AuditRecord>();
                AuditRecorder recorder = alternative ? value -> received.add(value) : received::add;
                context.withBean(AuditRecorder.class, () -> recorder).run(ctx -> {
                    assertNull(ctx.getStartupFailure());
                    ctx.getBean(Caller.class).record(new FixtureInput("fixture-actor", TIME, "SYNTHETIC-" + forbidden));
                    AuditRecord expected = new AuditRecord(new AuditEventType("fixture.audit-recording"),
                            new AuditActorId("fixture-actor"), TIME, new AuditOutcome("completed"),
                            null, null, null, new AuditMetadata(List.of(new AuditMetadataEntry("source", "standalone-fixture"))));
                    assertEquals(List.of(expected), received);
                    assertEquals(expected.hashCode(), received.getFirst().hashCode());
                });
            }
        }
    }

    @Test
    void missing_recorder_fails_consumer_wiring() {
        context.run(ctx -> assertNotNull(ctx.getStartupFailure()));
    }

    @Test
    void explicit_calls_and_recorder_failure_remain_observable() {
        var calls = new AtomicInteger();
        AuditRecorder success = value -> calls.incrementAndGet();
        context.withBean(AuditRecorder.class, () -> success).run(ctx -> {
            Caller caller = ctx.getBean(Caller.class);
            var input = new FixtureInput("actor", TIME, "synthetic");
            caller.record(input);
            caller.record(input);
            assertEquals(2, calls.get());
        });
        calls.set(0);
        var failure = new IllegalStateException("synthetic recorder failure");
        AuditRecorder throwing = value -> { calls.incrementAndGet(); throw failure; };
        context.withBean(AuditRecorder.class, () -> throwing).run(ctx -> {
            assertSame(failure, assertThrows(IllegalStateException.class, () -> ctx.getBean(Caller.class)
                    .record(new FixtureInput("actor", TIME, "synthetic"))));
            assertEquals(1, calls.get());
        });
    }

    @Test
    void invalid_mapper_input_never_reaches_recorder() {
        var calls = new AtomicInteger();
        AuditRecorder recorder = value -> calls.incrementAndGet();
        context.withBean(AuditRecorder.class, () -> recorder).run(ctx -> {
            Caller caller = ctx.getBean(Caller.class);
            assertThrows(NullPointerException.class, () -> caller.record(new FixtureInput(null, TIME, "synthetic")));
            for (String blank : List.of("", " ", "\t\n")) {
                assertThrows(IllegalArgumentException.class, () -> caller.record(new FixtureInput(blank, TIME, "synthetic")));
            }
            assertThrows(NullPointerException.class, () -> caller.record(new FixtureInput("actor", null, "synthetic")));
            assertEquals(0, calls.get());
        });
    }

    @Test
    void full_envelope_and_immutable_metadata_survive_artifact_boundary() {
        var input = new ArrayList<>(List.of(AuditMetadataEntry.of("key", "value")));
        AuditMetadata metadata = AuditMetadata.of(input);
        input.clear();
        AuditRecord record = AuditRecord.create(AuditEventType.of("custom"), AuditActorId.of("actor"), TIME,
                AuditOutcome.of("custom-result"), "tenant", "resource", "id", metadata);
        assertEquals(new AuditRecord(new AuditEventType("custom"), new AuditActorId("actor"), TIME,
                new AuditOutcome("custom-result"), "tenant", "resource", "id",
                new AuditMetadata(List.of(new AuditMetadataEntry("key", "value")))), record);
        assertThrows(UnsupportedOperationException.class, () -> record.metadata().entries().clear());
        AuditRecord minimal = AuditRecord.create(record.eventType(), record.actorId(), TIME, record.outcome());
        assertNull(minimal.tenantId());
        assertNull(minimal.resourceType());
        assertNull(minimal.resourceId());
        assertEquals(AuditMetadata.empty(), minimal.metadata());
    }

    @Test
    void artifact_declares_exact_supported_audit_api() throws Exception {
        var properties = new Properties();
        try (var stream = AuditRecord.class.getResourceAsStream("/META-INF/orca/backend-artifact-compatibility.properties")) {
            assertNotNull(stream, "Artifact compatibility metadata must exist");
            properties.load(stream);
        }
        String declaration = properties.getProperty("audit.public-api");
        assertNotNull(declaration, "Artifact must declare the supported audit API");
        Set<String> expected = Set.of(AuditActorId.class.getName(), AuditEventType.class.getName(),
                AuditMetadata.class.getName(), AuditMetadataEntry.class.getName(), AuditOutcome.class.getName(),
                AuditRecord.class.getName(), AuditRecorder.class.getName());
        List<String> declared = Arrays.stream(declaration.split(",")).map(String::trim).toList();
        assertEquals(7, declared.size());
        assertEquals(expected, new HashSet<>(declared));
    }
}
