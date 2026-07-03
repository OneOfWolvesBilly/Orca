package io.github.oneofwolvesbilly.orca.referencecore.application;

import io.github.oneofwolvesbilly.orca.auth.application.AuthSystemRoleDirectory;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthSystemRole;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedUserId;
import io.github.oneofwolvesbilly.orca.auth.domain.CurrentUserContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LookupClientDiagnosticUseCaseTest {

    private static final ClientFailureReferenceId REFERENCE_ID =
            ClientFailureReferenceId.of("7f1eb30a-86d0-4a3e-89c8-a6ff395ec144");
    private static final ClientDiagnosticRecord RECORD = ClientDiagnosticRecord.create(
            REFERENCE_ID,
            Instant.parse("2026-07-02T00:00:00Z"),
            ClientDiagnosticCategory.UNEXPECTED_RESPONSE,
            ClientOperation.PASSWORD_LOGIN,
            ClientApplication.REACT,
            200
    );

    @Test
    void it_admin_can_look_up_record_by_exact_reference() {
        var useCase = new LookupClientDiagnosticUseCase(
                repository(Optional.of(RECORD)),
                roleDirectory(true)
        );

        ClientDiagnosticRecord result = useCase.handle(
                CurrentUserContext.establish(AuthenticatedUserId.of("admin")),
                REFERENCE_ID
        );

        assertEquals(RECORD, result);
    }

    @Test
    void non_it_admin_cannot_look_up_record() {
        var useCase = new LookupClientDiagnosticUseCase(
                repository(Optional.of(RECORD)),
                roleDirectory(false)
        );

        assertThrows(ClientDiagnosticForbiddenException.class, () -> useCase.handle(
                CurrentUserContext.establish(AuthenticatedUserId.of("user")),
                REFERENCE_ID
        ));
    }

    @Test
    void unknown_reference_is_not_found() {
        var useCase = new LookupClientDiagnosticUseCase(
                repository(Optional.empty()),
                roleDirectory(true)
        );

        assertThrows(ClientDiagnosticNotFoundException.class, () -> useCase.handle(
                CurrentUserContext.establish(AuthenticatedUserId.of("admin")),
                REFERENCE_ID
        ));
    }

    private static AuthSystemRoleDirectory roleDirectory(boolean allowed) {
        return (authenticatedUserId, role) -> allowed && role == AuthSystemRole.IT_ADMIN;
    }

    private static ClientDiagnosticRecordRepository repository(Optional<ClientDiagnosticRecord> result) {
        return new ClientDiagnosticRecordRepository() {
            @Override
            public void save(ClientDiagnosticRecord record) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Optional<ClientDiagnosticRecord> findByReferenceId(ClientFailureReferenceId referenceId) {
                return result;
            }
        };
    }
}
