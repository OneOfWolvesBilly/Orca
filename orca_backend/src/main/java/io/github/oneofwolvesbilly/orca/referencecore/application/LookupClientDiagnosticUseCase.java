package io.github.oneofwolvesbilly.orca.referencecore.application;

import io.github.oneofwolvesbilly.orca.auth.application.AuthSystemRoleDirectory;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthSystemRole;
import io.github.oneofwolvesbilly.orca.auth.domain.CurrentUserContext;

import java.util.Objects;

public final class LookupClientDiagnosticUseCase {

    private final ClientDiagnosticRecordRepository repository;
    private final AuthSystemRoleDirectory authSystemRoleDirectory;

    public LookupClientDiagnosticUseCase(
            ClientDiagnosticRecordRepository repository,
            AuthSystemRoleDirectory authSystemRoleDirectory
    ) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.authSystemRoleDirectory = Objects.requireNonNull(authSystemRoleDirectory, "authSystemRoleDirectory");
    }

    public ClientDiagnosticRecord handle(
            CurrentUserContext currentUserContext,
            ClientFailureReferenceId referenceId
    ) {
        Objects.requireNonNull(currentUserContext, "currentUserContext");
        Objects.requireNonNull(referenceId, "referenceId");
        if (!authSystemRoleDirectory.hasRole(
                currentUserContext.authenticatedUserId(),
                AuthSystemRole.IT_ADMIN
        )) {
            throw new ClientDiagnosticForbiddenException();
        }
        return repository.findByReferenceId(referenceId)
                .orElseThrow(ClientDiagnosticNotFoundException::new);
    }
}
