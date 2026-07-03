package io.github.oneofwolvesbilly.orca.referencecore.application;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecordClientDiagnosticUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-07-02T00:00:00Z");
    private static final ClientFailureReferenceId REFERENCE_ID =
            ClientFailureReferenceId.of("7f1eb30a-86d0-4a3e-89c8-a6ff395ec144");

    @Test
    void persists_record_before_returning_server_generated_reference() {
        var repository = new FakeRepository();
        var useCase = new RecordClientDiagnosticUseCase(
                repository,
                () -> REFERENCE_ID,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        ClientFailureReferenceId result = useCase.handle(new RecordClientDiagnosticCommand(
                ClientDiagnosticCategory.MALFORMED_RESPONSE,
                ClientOperation.PASSWORD_LOGIN,
                ClientApplication.REACT,
                500
        ));

        assertEquals(REFERENCE_ID, result);
        ClientDiagnosticRecord saved = repository.findByReferenceId(REFERENCE_ID).orElseThrow();
        assertEquals(NOW, saved.occurredAt());
        assertEquals(ClientApplication.REACT, saved.clientApplication());
    }

    @Test
    void does_not_return_reference_when_persistence_fails() {
        var repository = new FakeRepository();
        repository.failOnSave = true;
        var useCase = new RecordClientDiagnosticUseCase(
                repository,
                () -> REFERENCE_ID,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        assertThrows(IllegalStateException.class, () -> useCase.handle(new RecordClientDiagnosticCommand(
                ClientDiagnosticCategory.TRANSPORT_FAILURE,
                ClientOperation.PASSWORD_LOGIN,
                ClientApplication.REACT,
                null
        )));
        assertFalse(repository.findByReferenceId(REFERENCE_ID).isPresent());
    }

    private static final class FakeRepository implements ClientDiagnosticRecordRepository {
        private ClientDiagnosticRecord record;
        private boolean failOnSave;

        @Override
        public void save(ClientDiagnosticRecord record) {
            if (failOnSave) {
                throw new IllegalStateException("persistence unavailable");
            }
            this.record = record;
        }

        @Override
        public Optional<ClientDiagnosticRecord> findByReferenceId(ClientFailureReferenceId referenceId) {
            return record != null && record.referenceId().equals(referenceId)
                    ? Optional.of(record)
                    : Optional.empty();
        }
    }
}
