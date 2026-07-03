package io.github.oneofwolvesbilly.orca.referencecore.application;

import java.util.Optional;

public interface ClientDiagnosticRecordRepository {

    void save(ClientDiagnosticRecord record);

    Optional<ClientDiagnosticRecord> findByReferenceId(ClientFailureReferenceId referenceId);
}
