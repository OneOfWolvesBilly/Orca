package io.github.oneofwolvesbilly.orca.auth.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProvisioningVerificationRequestTest {

    private static final ProvisioningVerificationRequestId REQUEST_ID =
            ProvisioningVerificationRequestId.of("3f1eb30a-86d0-4a3e-89c8-a6ff395ec144");
    private static final VerificationCode CODE = VerificationCode.of("123456");
    private static final Instant NOW = Instant.parse("2026-05-13T00:00:00Z");
    private static final Instant LATER = Instant.parse("2026-05-13T00:05:00Z");

    @Test
    void confirm_marks_pending_unexpired_request_as_verified_when_code_matches() {
        ProvisioningVerificationRequest request = ProvisioningVerificationRequest.pending(REQUEST_ID, CODE, LATER);

        request.confirm(CODE, NOW);

        assertTrue(request.isVerified());
    }

    @Test
    void confirm_rejects_code_mismatch() {
        ProvisioningVerificationRequest request = ProvisioningVerificationRequest.pending(REQUEST_ID, CODE, LATER);

        assertThrows(ProvisioningIdentityVerificationRejectedException.class, () ->
                request.confirm(VerificationCode.of("654321"), NOW)
        );

        assertFalse(request.isVerified());
    }

    @Test
    void confirm_rejects_expired_request() {
        ProvisioningVerificationRequest request = ProvisioningVerificationRequest.pending(REQUEST_ID, CODE, NOW);

        assertThrows(ProvisioningIdentityVerificationRejectedException.class, () ->
                request.confirm(CODE, LATER)
        );

        assertFalse(request.isVerified());
    }

    @Test
    void confirm_rejects_already_verified_request() {
        ProvisioningVerificationRequest request = ProvisioningVerificationRequest.verified(REQUEST_ID, CODE, LATER);

        assertThrows(ProvisioningIdentityVerificationRejectedException.class, () ->
                request.confirm(CODE, NOW)
        );

        assertTrue(request.isVerified());
    }
}
