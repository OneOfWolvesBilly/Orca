package io.github.oneofwolvesbilly.orca.organization.infrastructure.spring;

import io.github.oneofwolvesbilly.orca.auth.application.RegisteredUserIdentityRepository;
import io.github.oneofwolvesbilly.orca.organization.application.AcceptInvitationUseCase;
import io.github.oneofwolvesbilly.orca.organization.application.AuditRecorder;
import io.github.oneofwolvesbilly.orca.organization.application.CreateGroupUseCase;
import io.github.oneofwolvesbilly.orca.organization.application.GroupIdGenerator;
import io.github.oneofwolvesbilly.orca.organization.application.GroupRepository;
import io.github.oneofwolvesbilly.orca.organization.application.InviteMemberUseCase;
import io.github.oneofwolvesbilly.orca.organization.application.RegisteredUserDirectory;
import io.github.oneofwolvesbilly.orca.organization.application.RejectInvitationUseCase;
import io.github.oneofwolvesbilly.orca.organization.application.RevokeInvitationUseCase;
import io.github.oneofwolvesbilly.orca.organization.domain.GroupId;
import io.github.oneofwolvesbilly.orca.organization.infrastructure.inmemory.InMemoryAuditRecorder;
import io.github.oneofwolvesbilly.orca.organization.infrastructure.auth.AuthRegisteredUserDirectoryAdapter;
import io.github.oneofwolvesbilly.orca.organization.infrastructure.persistence.GroupEntityMapper;
import io.github.oneofwolvesbilly.orca.organization.infrastructure.persistence.JdbcGroupRepositoryAdapter;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.UUID;

@Configuration
class OrganizationConfiguration {

    @Bean
    GroupEntityMapper groupEntityMapper() {
        return new GroupEntityMapper();
    }

    @Bean
    Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();
        return flyway;
    }

    @Bean
    GroupRepository groupRepository(DataSource dataSource, GroupEntityMapper mapper, Flyway flyway) {
        return new JdbcGroupRepositoryAdapter(dataSource, mapper);
    }

    @Bean
    AuditRecorder auditRecorder() {
        return new InMemoryAuditRecorder();
    }

    @Bean
    GroupIdGenerator groupIdGenerator() {
        return () -> GroupId.of(UUID.randomUUID().toString());
    }

    @Bean
    RegisteredUserDirectory registeredUserDirectory(RegisteredUserIdentityRepository registeredUserIdentityRepository) {
        return new AuthRegisteredUserDirectoryAdapter(registeredUserIdentityRepository);
    }

    @Bean
    CreateGroupUseCase createGroupUseCase(
            GroupRepository groupRepository,
            AuditRecorder auditRecorder,
            GroupIdGenerator groupIdGenerator
    ) {
        return new CreateGroupUseCase(groupRepository, auditRecorder, groupIdGenerator);
    }

    @Bean
    InviteMemberUseCase inviteMemberUseCase(
            GroupRepository groupRepository,
            RegisteredUserDirectory registeredUserDirectory
    ) {
        return new InviteMemberUseCase(groupRepository, registeredUserDirectory);
    }

    @Bean
    AcceptInvitationUseCase acceptInvitationUseCase(GroupRepository groupRepository) {
        return new AcceptInvitationUseCase(groupRepository);
    }

    @Bean
    RejectInvitationUseCase rejectInvitationUseCase(GroupRepository groupRepository) {
        return new RejectInvitationUseCase(groupRepository);
    }

    @Bean
    RevokeInvitationUseCase revokeInvitationUseCase(GroupRepository groupRepository) {
        return new RevokeInvitationUseCase(groupRepository);
    }
}
