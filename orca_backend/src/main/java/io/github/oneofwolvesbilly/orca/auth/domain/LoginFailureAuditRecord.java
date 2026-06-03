package io.github.oneofwolvesbilly.orca.auth.domain;

import java.time.Instant;
import java.util.Objects;

/** Auth-owned server-side audit state for one rejected password login attempt. */
public record LoginFailureAuditRecord(
        LoginFailureReferenceId referenceId,
        Instant occurredAt,
        String submittedLoginIdentifier,
        LoginFailureReason reason
) {

    public LoginFailureAuditRecord {
        Objects.requireNonNull(referenceId, "referenceId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(reason, "reason");
    }

    public static LoginFailureAuditRecord create(
            LoginFailureReferenceId referenceId,
            Instant occurredAt,
            String submittedLoginIdentifier,
            LoginFailureReason reason
    ) {
        return new LoginFailureAuditRecord(referenceId, occurredAt, submittedLoginIdentifier, reason);
    }
}
