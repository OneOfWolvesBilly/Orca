package io.github.oneofwolvesbilly.orca.auth.infrastructure.spring;

import io.github.oneofwolvesbilly.orca.auth.application.EstablishCurrentUserContextUseCase;
import io.github.oneofwolvesbilly.orca.auth.web.CurrentUserContextResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class AuthConfiguration {

    @Bean
    EstablishCurrentUserContextUseCase establishCurrentUserContextUseCase() {
        return new EstablishCurrentUserContextUseCase();
    }

    @Bean
    CurrentUserContextResolver currentUserContextResolver(
            EstablishCurrentUserContextUseCase establishCurrentUserContextUseCase
    ) {
        return new CurrentUserContextResolver(establishCurrentUserContextUseCase);
    }
}
