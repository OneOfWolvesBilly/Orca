package io.github.oneofwolvesbilly.orca.referencecore.application;

import java.time.Instant;
import java.util.Objects;

public record ClientDiagnosticRecord(
        ClientFailureReferenceId referenceId,
        Instant occurredAt,
        ClientDiagnosticCategory category,
        ClientOperation operation,
        ClientApplication clientApplication,
        Integer responseStatus
) {

    public ClientDiagnosticRecord {
        Objects.requireNonNull(referenceId, "referenceId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(clientApplication, "clientApplication");
        if (responseStatus != null && (responseStatus < 100 || responseStatus > 599)) {
            throw new IllegalArgumentException("Response status must be a valid HTTP status");
        }
        if (category == ClientDiagnosticCategory.TRANSPORT_FAILURE && responseStatus != null) {
            throw new IllegalArgumentException("Transport failure must not include a response status");
        }
    }

    public static ClientDiagnosticRecord create(
            ClientFailureReferenceId referenceId,
            Instant occurredAt,
            ClientDiagnosticCategory category,
            ClientOperation operation,
            ClientApplication clientApplication,
            Integer responseStatus
    ) {
        return new ClientDiagnosticRecord(
                referenceId,
                occurredAt,
                category,
                operation,
                clientApplication,
                responseStatus
        );
    }
}
