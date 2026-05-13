package io.github.oneofwolvesbilly.orca.auth.application;

import io.github.oneofwolvesbilly.orca.auth.domain.ProvisioningIdentityVerificationRejectedException;
import io.github.oneofwolvesbilly.orca.auth.domain.ProvisioningVerificationRequest;
import io.github.oneofwolvesbilly.orca.auth.domain.ProvisioningVerificationRequestId;
import io.github.oneofwolvesbilly.orca.auth.domain.VerificationCode;
import io.github.oneofwolvesbilly.orca.auth.support.FakeProvisioningVerificationRequestRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfirmProvisioningIdentityVerificationUseCaseTest {

    private static final String REQUEST_ID = "3f1eb30a-86d0-4a3e-89c8-a6ff395ec144";
    private static final Instant NOW = Instant.parse("2026-05-13T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void handle_verifies_and_saves_pending_request_when_code_matches() {
        var request = ProvisioningVerificationRequest.pending(
                ProvisioningVerificationRequestId.of(REQUEST_ID),
                VerificationCode.of("123456"),
                NOW.plusSeconds(300)
        );
        var repository = new FakeProvisioningVerificationRequestRepository().store(request);
        var useCase = new ConfirmProvisioningIdentityVerificationUseCase(repository, CLOCK);

        useCase.handle(new ConfirmProvisioningIdentityVerificationCommand(REQUEST_ID, "123456"));

        assertTrue(request.isVerified());
        assertSame(request, repository.savedRequest());
    }

    @Test
    void handle_rejects_unknown_request_with_verification_failure() {
        var useCase = new ConfirmProvisioningIdentityVerificationUseCase(
                new FakeProvisioningVerificationRequestRepository(),
                CLOCK
        );

        assertThrows(ProvisioningIdentityVerificationRejectedException.class, () ->
                useCase.handle(new ConfirmProvisioningIdentityVerificationCommand(REQUEST_ID, "123456"))
        );
    }

    @Test
    void handle_rejects_malformed_request_id_with_verification_failure() {
        var useCase = new ConfirmProvisioningIdentityVerificationUseCase(
                new FakeProvisioningVerificationRequestRepository(),
                CLOCK
        );

        assertThrows(ProvisioningIdentityVerificationRejectedException.class, () ->
                useCase.handle(new ConfirmProvisioningIdentityVerificationCommand("not-a-uuid", "123456"))
        );
    }

    @Test
    void handle_uses_same_failure_for_expired_already_verified_and_code_mismatch() {
        var expired = ProvisioningVerificationRequest.pending(
                ProvisioningVerificationRequestId.of("3f1eb30a-86d0-4a3e-89c8-a6ff395ec144"),
                VerificationCode.of("123456"),
                NOW.minusSeconds(1)
        );
        var alreadyVerified = ProvisioningVerificationRequest.verified(
                ProvisioningVerificationRequestId.of("064a4ee7-b38f-4a76-b925-263caa20832f"),
                VerificationCode.of("123456"),
                NOW.plusSeconds(300)
        );
        var codeMismatch = ProvisioningVerificationRequest.pending(
                ProvisioningVerificationRequestId.of("f7d71524-c98d-4f82-ae2e-858e94b62a13"),
                VerificationCode.of("123456"),
                NOW.plusSeconds(300)
        );
        var repository = new FakeProvisioningVerificationRequestRepository()
                .store(expired)
                .store(alreadyVerified)
                .store(codeMismatch);
        var useCase = new ConfirmProvisioningIdentityVerificationUseCase(repository, CLOCK);

        assertThrows(ProvisioningIdentityVerificationRejectedException.class, () ->
                useCase.handle(new ConfirmProvisioningIdentityVerificationCommand(expired.id().toString(), "123456"))
        );
        assertThrows(ProvisioningIdentityVerificationRejectedException.class, () ->
                useCase.handle(new ConfirmProvisioningIdentityVerificationCommand(alreadyVerified.id().toString(), "123456"))
        );
        assertThrows(ProvisioningIdentityVerificationRejectedException.class, () ->
                useCase.handle(new ConfirmProvisioningIdentityVerificationCommand(codeMismatch.id().toString(), "654321"))
        );
    }
}
