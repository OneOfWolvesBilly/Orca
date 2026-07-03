package io.github.oneofwolvesbilly.orca.referencecore.application;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClientDiagnosticRecordTest {

    private static final ClientFailureReferenceId REFERENCE_ID =
            ClientFailureReferenceId.of("7f1eb30a-86d0-4a3e-89c8-a6ff395ec144");
    private static final Instant OCCURRED_AT = Instant.parse("2026-07-02T00:00:00Z");

    @Test
    void creates_allowlisted_client_diagnostic_record() {
        ClientDiagnosticRecord record = ClientDiagnosticRecord.create(
                REFERENCE_ID,
                OCCURRED_AT,
                ClientDiagnosticCategory.MALFORMED_RESPONSE,
                ClientOperation.PASSWORD_LOGIN,
                ClientApplication.REACT,
                500
        );

        assertEquals(REFERENCE_ID, record.referenceId());
        assertEquals(OCCURRED_AT, record.occurredAt());
        assertEquals(500, record.responseStatus());
    }

    @Test
    void rejects_response_status_outside_http_range() {
        assertThrows(IllegalArgumentException.class, () -> ClientDiagnosticRecord.create(
                REFERENCE_ID,
                OCCURRED_AT,
                ClientDiagnosticCategory.UNEXPECTED_RESPONSE,
                ClientOperation.PASSWORD_LOGIN,
                ClientApplication.REACT,
                600
        ));
    }

    @Test
    void transport_failure_rejects_response_status() {
        assertThrows(IllegalArgumentException.class, () -> ClientDiagnosticRecord.create(
                REFERENCE_ID,
                OCCURRED_AT,
                ClientDiagnosticCategory.TRANSPORT_FAILURE,
                ClientOperation.PASSWORD_LOGIN,
                ClientApplication.REACT,
                503
        ));
    }

    @Test
    void client_failure_reference_must_be_uuid() {
        assertThrows(IllegalArgumentException.class, () -> ClientFailureReferenceId.of("contains-details"));
    }
}
