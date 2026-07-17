package io.github.oneofwolvesbilly.orcafixture;

import io.github.oneofwolvesbilly.orca.auth.api.EnableOrcaEmbeddedAuth;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableOrcaEmbeddedAuth
public class MinimalConsumerFixtureApplication {

    public static void main(String[] args) {
        SpringApplication.run(MinimalConsumerFixtureApplication.class, args);
    }

    @Bean
    FixtureActorCommand fixtureActorCommand() {
        return actorId -> {
        };
    }
}
