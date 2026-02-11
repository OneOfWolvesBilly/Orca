package io.github.oneofwolvesbilly.orca.organization.infrastructure.inmemory;

import io.github.oneofwolvesbilly.orca.organization.application.AuditEvent;
import io.github.oneofwolvesbilly.orca.organization.application.AuditRecorder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Stores audit events in memory for tests and local runs. */
public final class InMemoryAuditRecorder implements AuditRecorder {

    private final List<AuditEvent> recordedEvents = new ArrayList<>();

    @Override
    public void record(AuditEvent event) {
        recordedEvents.add(Objects.requireNonNull(event, "event"));
    }

    /** Returns recorded events for assertions. */
    public List<AuditEvent> recordedEvents() {
        return Collections.unmodifiableList(recordedEvents);
    }
}
