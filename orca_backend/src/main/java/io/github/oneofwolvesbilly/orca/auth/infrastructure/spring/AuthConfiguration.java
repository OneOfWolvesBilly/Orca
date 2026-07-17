package io.github.oneofwolvesbilly.orca.auth.infrastructure.spring;

import io.github.oneofwolvesbilly.orca.auth.application.AuthenticatedSessionIdGenerator;
import io.github.oneofwolvesbilly.orca.auth.application.AuthenticatedSessionRepository;
import io.github.oneofwolvesbilly.orca.auth.application.EstablishCurrentUserContextUseCase;
import io.github.oneofwolvesbilly.orca.auth.application.AuthSystemRoleDirectory;
import io.github.oneofwolvesbilly.orca.auth.application.ConfirmProvisioningIdentityVerificationUseCase;
import io.github.oneofwolvesbilly.orca.auth.application.LoginFailureAuditRecordRepository;
import io.github.oneofwolvesbilly.orca.auth.application.LoginFailureReferenceIdGenerator;
import io.github.oneofwolvesbilly.orca.auth.application.LoginCredentialVerifier;
import io.github.oneofwolvesbilly.orca.auth.application.LogoutSessionUseCase;
import io.github.oneofwolvesbilly.orca.auth.application.PasswordLoginUseCase;
import io.github.oneofwolvesbilly.orca.auth.application.ProvisionRegisteredUserIdentityUseCase;
import io.github.oneofwolvesbilly.orca.auth.application.ProvisioningVerificationRequestRepository;
import io.github.oneofwolvesbilly.orca.auth.application.RegisteredUserIdentityRepository;
import io.github.oneofwolvesbilly.orca.auth.application.ResolveCurrentUserContextFromSessionUseCase;
import io.github.oneofwolvesbilly.orca.auth.infrastructure.persistence.JdbcAuthenticatedSessionRepository;
import io.github.oneofwolvesbilly.orca.auth.infrastructure.persistence.JdbcAuthSystemRoleDirectory;
import io.github.oneofwolvesbilly.orca.auth.infrastructure.persistence.JdbcLoginFailureAuditRecordRepository;
import io.github.oneofwolvesbilly.orca.auth.infrastructure.persistence.JdbcLoginCredentialVerifier;
import io.github.oneofwolvesbilly.orca.auth.infrastructure.persistence.JdbcProvisioningVerificationRequestRepository;
import io.github.oneofwolvesbilly.orca.auth.infrastructure.persistence.JdbcRegisteredUserIdentityRepository;
import io.github.oneofwolvesbilly.orca.auth.infrastructure.persistence.UuidAuthenticatedSessionIdGenerator;
import io.github.oneofwolvesbilly.orca.auth.infrastructure.persistence.UuidLoginFailureReferenceIdGenerator;
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
import java.time.Clock;
import java.time.Duration;
import java.util.List;

@Configuration
class AuthConfiguration {

    @Bean
    RegisteredUserIdentityRepository registeredUserIdentityRepository(DataSource dataSource) {
        return new JdbcRegisteredUserIdentityRepository(dataSource);
    }

    @Bean
    AuthSystemRoleDirectory authSystemRoleDirectory(DataSource dataSource) {
        return new JdbcAuthSystemRoleDirectory(dataSource);
    }

    @Bean
    ProvisioningVerificationRequestRepository provisioningVerificationRequestRepository(DataSource dataSource) {
        return new JdbcProvisioningVerificationRequestRepository(dataSource);
    }

    @Bean
    LoginCredentialVerifier loginCredentialVerifier(DataSource dataSource) {
        return new JdbcLoginCredentialVerifier(dataSource);
    }

    @Bean
    AuthenticatedSessionRepository authenticatedSessionRepository(DataSource dataSource) {
        return new JdbcAuthenticatedSessionRepository(dataSource);
    }

    @Bean
    LoginFailureAuditRecordRepository loginFailureAuditRecordRepository(DataSource dataSource) {
        return new JdbcLoginFailureAuditRecordRepository(dataSource);
    }

    @Bean
    AuthenticatedSessionIdGenerator authenticatedSessionIdGenerator() {
        return new UuidAuthenticatedSessionIdGenerator();
    }

    @Bean
    LoginFailureReferenceIdGenerator loginFailureReferenceIdGenerator() {
        return new UuidLoginFailureReferenceIdGenerator();
    }

    @Bean
    Clock authClock() {
        return Clock.systemUTC();
    }

    @Bean
    Duration authSessionLifetime() {
        return Duration.ofHours(8);
    }

    @Bean
    EstablishCurrentUserContextUseCase establishCurrentUserContextUseCase(
            RegisteredUserIdentityRepository registeredUserIdentityRepository
    ) {
        return new EstablishCurrentUserContextUseCase(registeredUserIdentityRepository);
    }

    @Bean
    ProvisionRegisteredUserIdentityUseCase provisionRegisteredUserIdentityUseCase(
            RegisteredUserIdentityRepository registeredUserIdentityRepository,
            AuthSystemRoleDirectory authSystemRoleDirectory
    ) {
        return new ProvisionRegisteredUserIdentityUseCase(registeredUserIdentityRepository, authSystemRoleDirectory);
    }

    @Bean
    ConfirmProvisioningIdentityVerificationUseCase confirmProvisioningIdentityVerificationUseCase(
            ProvisioningVerificationRequestRepository provisioningVerificationRequestRepository,
            Clock authClock
    ) {
        return new ConfirmProvisioningIdentityVerificationUseCase(provisioningVerificationRequestRepository, authClock);
    }

    @Bean
    PasswordLoginUseCase passwordLoginUseCase(
            LoginCredentialVerifier loginCredentialVerifier,
            AuthenticatedSessionRepository authenticatedSessionRepository,
            AuthenticatedSessionIdGenerator authenticatedSessionIdGenerator,
            LoginFailureAuditRecordRepository loginFailureAuditRecordRepository,
            LoginFailureReferenceIdGenerator loginFailureReferenceIdGenerator,
            Clock authClock,
            Duration authSessionLifetime
    ) {
        return new PasswordLoginUseCase(
                loginCredentialVerifier,
                authenticatedSessionRepository,
                authenticatedSessionIdGenerator,
                loginFailureAuditRecordRepository,
                loginFailureReferenceIdGenerator,
                authClock,
                authSessionLifetime
        );
    }

    @Bean
    LogoutSessionUseCase logoutSessionUseCase(
            AuthenticatedSessionRepository authenticatedSessionRepository,
            Clock authClock
    ) {
        return new LogoutSessionUseCase(authenticatedSessionRepository, authClock);
    }

    @Bean
    ResolveCurrentUserContextFromSessionUseCase resolveCurrentUserContextFromSessionUseCase(
            AuthenticatedSessionRepository authenticatedSessionRepository,
            EstablishCurrentUserContextUseCase establishCurrentUserContextUseCase,
            Clock authClock
    ) {
        return new ResolveCurrentUserContextFromSessionUseCase(
                authenticatedSessionRepository,
                establishCurrentUserContextUseCase,
                authClock
        );
    }

    @Bean
    CurrentUserContextResolver currentUserContextResolver(
            ResolveCurrentUserContextFromSessionUseCase resolveCurrentUserContextFromSessionUseCase
    ) {
        return new CurrentUserContextResolver(resolveCurrentUserContextFromSessionUseCase);
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
                        .addPathPatterns("/**");
            }

            @Override
            public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                resolvers.add(new CurrentUserContextArgumentResolver());
            }
        };
    }
}
