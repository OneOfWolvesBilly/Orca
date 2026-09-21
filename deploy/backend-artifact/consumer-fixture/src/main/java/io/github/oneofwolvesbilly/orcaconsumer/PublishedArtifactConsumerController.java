package io.github.oneofwolvesbilly.orcaconsumer;

import io.github.oneofwolvesbilly.orca.auth.api.AuthenticatedActor;
import io.github.oneofwolvesbilly.orca.auth.api.OrcaProtectedCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fixture")
final class PublishedArtifactConsumerController {

    private final RecordingActorCommand command;

    PublishedArtifactConsumerController(RecordingActorCommand command) {
        this.command = command;
    }

    @PostMapping("/actor-context-check")
    @OrcaProtectedCommand
    ResponseEntity<Void> check(AuthenticatedActor actor) {
        command.handle(actor.actorId());
        return ResponseEntity.noContent().build();
    }
}
