package io.github.oneofwolvesbilly.orca.organization.application;


import java.util.ArrayList;
import java.util.List;

public final class InMemoryAuditRecorder implements AuditRecorder {

    private final List<AuditEvent> events = new ArrayList<>();

    @Override
    public void record(AuditEvent event) {
        events.add(event);
    }

    public List<AuditEvent> events() {
        return List.copyOf(events);
    }
}