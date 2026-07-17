package io.github.oneofwolvesbilly.orca.referencecore.application;

@FunctionalInterface
public interface AuditRecorder {

    void record(AuditRecord record);
}
