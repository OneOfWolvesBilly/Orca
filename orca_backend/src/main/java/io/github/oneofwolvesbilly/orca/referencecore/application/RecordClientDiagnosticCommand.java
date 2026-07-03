package io.github.oneofwolvesbilly.orca.referencecore.application;

public record RecordClientDiagnosticCommand(
        ClientDiagnosticCategory category,
        ClientOperation operation,
        ClientApplication clientApplication,
        Integer responseStatus
) {

    public RecordClientDiagnosticCommand {
        if (category == null || operation == null || clientApplication == null) {
            throw new IllegalArgumentException("Required client diagnostic field is missing");
        }
    }
}
