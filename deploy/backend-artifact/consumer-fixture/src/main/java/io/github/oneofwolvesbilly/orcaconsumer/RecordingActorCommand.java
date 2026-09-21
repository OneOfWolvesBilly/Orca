package io.github.oneofwolvesbilly.orcaconsumer;

import java.util.ArrayList;
import java.util.List;

final class RecordingActorCommand {

    private final List<String> actorIds = new ArrayList<>();

    void handle(String actorId) {
        actorIds.add(actorId);
    }

    void reset() {
        actorIds.clear();
    }

    List<String> actorIds() {
        return List.copyOf(actorIds);
    }
}
