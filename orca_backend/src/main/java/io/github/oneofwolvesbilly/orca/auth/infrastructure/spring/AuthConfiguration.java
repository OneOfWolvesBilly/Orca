package io.github.oneofwolvesbilly.orca.auth.infrastructure.spring;

import io.github.oneofwolvesbilly.orca.auth.application.EstablishCurrentUserContextUseCase;
import io.github.oneofwolvesbilly.orca.auth.web.CurrentUserContextArgumentResolver;
import io.github.oneofwolvesbilly.orca.auth.web.CurrentUserContextInterceptor;
import io.github.oneofwolvesbilly.orca.auth.web.CurrentUserContextResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

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

    @Bean
    HandlerInterceptor currentUserContextInterceptor(CurrentUserContextResolver currentUserContextResolver) {
        return new CurrentUserContextInterceptor(currentUserContextResolver);
    }

    @Bean
    WebMvcConfigurer authWebMvcConfigurer(HandlerInterceptor currentUserContextInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(currentUserContextInterceptor).addPathPatterns("/api/**");
            }

            @Override
            public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                resolvers.add(new CurrentUserContextArgumentResolver());
            }
        };
    }
}
