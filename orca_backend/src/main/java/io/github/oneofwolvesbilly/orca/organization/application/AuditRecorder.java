package io.github.oneofwolvesbilly.orca.organization.application;

public interface AuditRecorder {
    void record(AuditEvent event);
}