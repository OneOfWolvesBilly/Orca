package io.github.oneofwolvesbilly.orca.organization.application;

/** Records audit events. */
public interface AuditRecorder {
    void record(AuditEvent event);
}