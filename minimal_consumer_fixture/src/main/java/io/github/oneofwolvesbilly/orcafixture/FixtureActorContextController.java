package io.github.oneofwolvesbilly.orcafixture;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.github.oneofwolvesbilly.orca.auth.api.AuthenticatedActor;
import io.github.oneofwolvesbilly.orca.auth.api.OrcaProtectedCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/fixture")
final class FixtureActorContextController {

    private final FixtureActorCommand fixtureActorCommand;

    FixtureActorContextController(FixtureActorCommand fixtureActorCommand) {
        this.fixtureActorCommand = Objects.requireNonNull(fixtureActorCommand, "fixtureActorCommand");
    }

    @PostMapping("/actor-context-check")
    @OrcaProtectedCommand
    ResponseEntity<Void> check(
            AuthenticatedActor authenticatedActor,
            @RequestBody EmptyRequest request
    ) {
        fixtureActorCommand.handle(authenticatedActor.actorId());
        return ResponseEntity.noContent().build();
    }

    record EmptyRequest() {

        @JsonAnySetter
        void rejectUnknownField(String name, Object value) {
            throw new IllegalArgumentException("fixture request body must be empty");
        }
    }
}
