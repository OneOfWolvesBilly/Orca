package io.github.oneofwolvesbilly.orca.referencecore.application;

public record RecordClientDiagnosticCommand(
        ClientDiagnosticCategory category,
        ClientOperation operation,
        ClientApplication clientApplication,
        Integer responseStatus
) {

    public RecordClientDiagnosticCommand {
        if (category == null || operation == null || clientApplication == null) {
            throw new ClientDiagnosticValidationException("Required client diagnostic field is missing");
        }
        if (responseStatus != null && (responseStatus < 100 || responseStatus > 599)) {
            throw new ClientDiagnosticValidationException("Response status must be a valid HTTP status");
        }
        if (category == ClientDiagnosticCategory.TRANSPORT_FAILURE && responseStatus != null) {
            throw new ClientDiagnosticValidationException("Transport failure must not include a response status");
        }
    }
}
