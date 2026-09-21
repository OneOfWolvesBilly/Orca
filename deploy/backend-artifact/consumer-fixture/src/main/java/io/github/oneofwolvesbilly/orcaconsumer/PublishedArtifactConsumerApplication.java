package io.github.oneofwolvesbilly.orcaconsumer;

import io.github.oneofwolvesbilly.orca.auth.api.EnableOrcaEmbeddedAuth;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableOrcaEmbeddedAuth
public class PublishedArtifactConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PublishedArtifactConsumerApplication.class, args);
    }

    @Bean
    RecordingActorCommand recordingActorCommand() {
        return new RecordingActorCommand();
    }
}
