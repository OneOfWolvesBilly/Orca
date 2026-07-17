package io.github.oneofwolvesbilly.orcafixture;

@FunctionalInterface
interface FixtureActorCommand {

    void handle(String actorId);
}
