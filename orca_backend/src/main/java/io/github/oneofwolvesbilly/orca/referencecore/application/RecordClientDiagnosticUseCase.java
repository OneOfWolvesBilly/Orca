package io.github.oneofwolvesbilly.orca.referencecore.application;

import java.time.Clock;
import java.util.Objects;

public final class RecordClientDiagnosticUseCase {

    private final ClientDiagnosticRecordRepository repository;
    private final ClientFailureReferenceIdGenerator referenceIdGenerator;
    private final Clock clock;

    public RecordClientDiagnosticUseCase(
            ClientDiagnosticRecordRepository repository,
            ClientFailureReferenceIdGenerator referenceIdGenerator,
            Clock clock
    ) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.referenceIdGenerator = Objects.requireNonNull(referenceIdGenerator, "referenceIdGenerator");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public ClientFailureReferenceId handle(RecordClientDiagnosticCommand command) {
        Objects.requireNonNull(command, "command");
        ClientDiagnosticRecord record = ClientDiagnosticRecord.create(
                referenceIdGenerator.generate(),
                clock.instant(),
                command.category(),
                command.operation(),
                command.clientApplication(),
                command.responseStatus()
        );
        repository.save(record);
        return record.referenceId();
    }
}
