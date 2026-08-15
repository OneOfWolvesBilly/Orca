package io.github.oneofwolvesbilly.orca.referencecore.web;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.github.oneofwolvesbilly.orca.auth.api.OrcaProtectedCommand;
import io.github.oneofwolvesbilly.orca.auth.domain.CurrentUserContext;
import io.github.oneofwolvesbilly.orca.referencecore.application.ClientApplication;
import io.github.oneofwolvesbilly.orca.referencecore.application.ClientDiagnosticCategory;
import io.github.oneofwolvesbilly.orca.referencecore.application.ClientDiagnosticNotFoundException;
import io.github.oneofwolvesbilly.orca.referencecore.application.ClientDiagnosticRecord;
import io.github.oneofwolvesbilly.orca.referencecore.application.ClientFailureReferenceId;
import io.github.oneofwolvesbilly.orca.referencecore.application.ClientOperation;
import io.github.oneofwolvesbilly.orca.referencecore.application.LookupClientDiagnosticUseCase;
import io.github.oneofwolvesbilly.orca.referencecore.application.RecordClientDiagnosticCommand;
import io.github.oneofwolvesbilly.orca.referencecore.application.RecordClientDiagnosticUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Objects;

@RestController
@RequestMapping("/api/client-diagnostics")
final class ClientDiagnosticController {

    private final RecordClientDiagnosticUseCase recordClientDiagnosticUseCase;
    private final LookupClientDiagnosticUseCase lookupClientDiagnosticUseCase;

    ClientDiagnosticController(
            RecordClientDiagnosticUseCase recordClientDiagnosticUseCase,
            LookupClientDiagnosticUseCase lookupClientDiagnosticUseCase
    ) {
        this.recordClientDiagnosticUseCase =
                Objects.requireNonNull(recordClientDiagnosticUseCase, "recordClientDiagnosticUseCase");
        this.lookupClientDiagnosticUseCase =
                Objects.requireNonNull(lookupClientDiagnosticUseCase, "lookupClientDiagnosticUseCase");
    }

    @PostMapping
    ResponseEntity<ClientDiagnosticCreatedResponse> record(@RequestBody ClientDiagnosticRequest request) {
        ClientFailureReferenceId referenceId = recordClientDiagnosticUseCase.handle(
                new RecordClientDiagnosticCommand(
                        request.category(),
                        request.operation(),
                        request.clientApplication(),
                        request.responseStatus()
                )
        );
        return ResponseEntity.status(201)
                .body(new ClientDiagnosticCreatedResponse(referenceId.value()));
    }

    @PostMapping("/lookup")
    @OrcaProtectedCommand
    ClientDiagnosticResponse lookup(
            @RequestBody ClientDiagnosticLookupRequest request,
            CurrentUserContext currentUserContext
    ) {
        ClientFailureReferenceId referenceId;
        try {
            referenceId = ClientFailureReferenceId.of(request.clientFailureReferenceId());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ClientDiagnosticNotFoundException();
        }
        return ClientDiagnosticResponse.from(
                lookupClientDiagnosticUseCase.handle(currentUserContext, referenceId)
        );
    }

    record ClientDiagnosticRequest(
            ClientDiagnosticCategory category,
            ClientOperation operation,
            ClientApplication clientApplication,
            Integer responseStatus
    ) {
        @JsonAnySetter
        void rejectUnknownField(String name, Object value) {
            throw new RequestValidationException("Unknown diagnostic field");
        }
    }

    record ClientDiagnosticCreatedResponse(String clientFailureReferenceId) {
    }

    record ClientDiagnosticLookupRequest(String clientFailureReferenceId) {
        @JsonAnySetter
        void rejectUnknownField(String name, Object value) {
            throw new RequestValidationException("Unknown lookup field");
        }
    }

    record ClientDiagnosticResponse(
            String clientFailureReferenceId,
            Instant occurredAt,
            ClientDiagnosticCategory category,
            ClientOperation operation,
            ClientApplication clientApplication,
            Integer responseStatus
    ) {
        static ClientDiagnosticResponse from(ClientDiagnosticRecord record) {
            return new ClientDiagnosticResponse(
                    record.referenceId().value(),
                    record.occurredAt(),
                    record.category(),
                    record.operation(),
                    record.clientApplication(),
                    record.responseStatus()
            );
        }
    }
}
