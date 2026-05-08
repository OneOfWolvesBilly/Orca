package io.github.oneofwolvesbilly.orca.auth.infrastructure.spring;

import io.github.oneofwolvesbilly.orca.auth.application.EstablishCurrentUserContextUseCase;
import io.github.oneofwolvesbilly.orca.auth.application.RegisteredUserIdentityRepository;
import io.github.oneofwolvesbilly.orca.auth.infrastructure.persistence.JdbcRegisteredUserIdentityRepository;
import io.github.oneofwolvesbilly.orca.auth.web.CurrentUserContextArgumentResolver;
import io.github.oneofwolvesbilly.orca.auth.web.CurrentUserContextInterceptor;
import io.github.oneofwolvesbilly.orca.auth.web.CurrentUserContextResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;
import java.util.List;

@Configuration
class AuthConfiguration {

    private static final String[] PROTECTED_HTTP_COMMAND_PATHS = {
            "/api/groups",
            "/api/groups/{groupId}/invitations",
            "/api/group-invitations/{invitationId}/accept",
            "/api/group-invitations/{invitationId}/reject",
            "/api/group-invitations/{invitationId}/revoke"
    };

    @Bean
    RegisteredUserIdentityRepository registeredUserIdentityRepository(DataSource dataSource) {
        return new JdbcRegisteredUserIdentityRepository(dataSource);
    }

    @Bean
    EstablishCurrentUserContextUseCase establishCurrentUserContextUseCase(
            RegisteredUserIdentityRepository registeredUserIdentityRepository
    ) {
        return new EstablishCurrentUserContextUseCase(registeredUserIdentityRepository);
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
                registry.addInterceptor(currentUserContextInterceptor)
                        .addPathPatterns(PROTECTED_HTTP_COMMAND_PATHS);
            }

            @Override
            public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                resolvers.add(new CurrentUserContextArgumentResolver());
            }
        };
    }
}
