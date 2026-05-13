package io.github.oneofwolvesbilly.orca.auth.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProvisioningVerificationRequestIdTest {

    @Test
    void of_accepts_uuid_string() {
        UUID value = UUID.fromString("3f1eb30a-86d0-4a3e-89c8-a6ff395ec144");

        ProvisioningVerificationRequestId id =
                ProvisioningVerificationRequestId.of("3f1eb30a-86d0-4a3e-89c8-a6ff395ec144");

        assertEquals(value, id.value());
    }

    @Test
    void of_rejects_malformed_uuid() {
        assertThrows(IllegalArgumentException.class, () ->
                ProvisioningVerificationRequestId.of("not-a-uuid")
        );
    }

    @Test
    void of_rejects_null_uuid() {
        assertThrows(NullPointerException.class, () -> ProvisioningVerificationRequestId.of(null));
    }
}
