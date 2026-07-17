package io.github.oneofwolvesbilly.orca.referencecore.support;

import io.github.oneofwolvesbilly.orca.referencecore.application.AuditRecord;
import io.github.oneofwolvesbilly.orca.referencecore.application.AuditRecorder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RecordingAuditRecorder implements AuditRecorder {

    private final List<AuditRecord> records = new ArrayList<>();

    @Override
    public void record(AuditRecord record) {
        records.add(Objects.requireNonNull(record, "record"));
    }

    public List<AuditRecord> records() {
        return List.copyOf(records);
    }
}
