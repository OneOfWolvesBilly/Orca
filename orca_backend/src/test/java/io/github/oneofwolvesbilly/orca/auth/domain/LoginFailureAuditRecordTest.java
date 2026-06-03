package io.github.oneofwolvesbilly.orca.auth.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginFailureAuditRecordTest {

    private static final LoginFailureReferenceId REFERENCE_ID =
            LoginFailureReferenceId.of("7f1eb30a-86d0-4a3e-89c8-a6ff395ec144");
    private static final Instant OCCURRED_AT = Instant.parse("2026-06-03T00:00:00Z");

    @Test
    void create_records_failed_login_troubleshooting_state_server_side() {
        LoginFailureAuditRecord record = LoginFailureAuditRecord.create(
                REFERENCE_ID,
                OCCURRED_AT,
                "employee-login-001",
                LoginFailureReason.INVALID_CREDENTIALS
        );

        assertEquals(REFERENCE_ID, record.referenceId());
        assertEquals(OCCURRED_AT, record.occurredAt());
        assertEquals("employee-login-001", record.submittedLoginIdentifier());
        assertEquals(LoginFailureReason.INVALID_CREDENTIALS, record.reason());
    }

    @Test
    void create_allows_missing_login_identifier_to_remain_server_side_audit_detail() {
        LoginFailureAuditRecord record = LoginFailureAuditRecord.create(
                REFERENCE_ID,
                OCCURRED_AT,
                null,
                LoginFailureReason.INVALID_INPUT
        );

        assertNull(record.submittedLoginIdentifier());
        assertEquals(LoginFailureReason.INVALID_INPUT, record.reason());
    }

    @Test
    void create_requires_reference_timestamp_and_reason() {
        assertThrows(NullPointerException.class, () ->
                LoginFailureAuditRecord.create(null, OCCURRED_AT, "login", LoginFailureReason.INVALID_INPUT)
        );
        assertThrows(NullPointerException.class, () ->
                LoginFailureAuditRecord.create(REFERENCE_ID, null, "login", LoginFailureReason.INVALID_INPUT)
        );
        assertThrows(NullPointerException.class, () ->
                LoginFailureAuditRecord.create(REFERENCE_ID, OCCURRED_AT, "login", null)
        );
    }
}
