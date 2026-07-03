package io.github.oneofwolvesbilly.orca.referencecore.infrastructure.spring;

import io.github.oneofwolvesbilly.orca.auth.application.AuthSystemRoleDirectory;
import io.github.oneofwolvesbilly.orca.referencecore.application.ClientDiagnosticRecordRepository;
import io.github.oneofwolvesbilly.orca.referencecore.application.ClientFailureReferenceIdGenerator;
import io.github.oneofwolvesbilly.orca.referencecore.application.LookupClientDiagnosticUseCase;
import io.github.oneofwolvesbilly.orca.referencecore.application.RecordClientDiagnosticUseCase;
import io.github.oneofwolvesbilly.orca.referencecore.infrastructure.persistence.JdbcClientDiagnosticRecordRepository;
import io.github.oneofwolvesbilly.orca.referencecore.infrastructure.persistence.UuidClientFailureReferenceIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.time.Clock;

@Configuration
class ReferenceCoreConfiguration {

    @Bean
    ClientDiagnosticRecordRepository clientDiagnosticRecordRepository(DataSource dataSource) {
        return new JdbcClientDiagnosticRecordRepository(dataSource);
    }

    @Bean
    ClientFailureReferenceIdGenerator clientFailureReferenceIdGenerator() {
        return new UuidClientFailureReferenceIdGenerator();
    }

    @Bean
    RecordClientDiagnosticUseCase recordClientDiagnosticUseCase(
            ClientDiagnosticRecordRepository repository,
            ClientFailureReferenceIdGenerator referenceIdGenerator,
            Clock authClock
    ) {
        return new RecordClientDiagnosticUseCase(repository, referenceIdGenerator, authClock);
    }

    @Bean
    LookupClientDiagnosticUseCase lookupClientDiagnosticUseCase(
            ClientDiagnosticRecordRepository repository,
            AuthSystemRoleDirectory authSystemRoleDirectory
    ) {
        return new LookupClientDiagnosticUseCase(repository, authSystemRoleDirectory);
    }
}
