package io.github.oneofwolvesbilly.orca.referencecore.infrastructure.persistence;

import io.github.oneofwolvesbilly.orca.referencecore.application.ClientApplication;
import io.github.oneofwolvesbilly.orca.referencecore.application.ClientDiagnosticCategory;
import io.github.oneofwolvesbilly.orca.referencecore.application.ClientDiagnosticRecord;
import io.github.oneofwolvesbilly.orca.referencecore.application.ClientFailureReferenceId;
import io.github.oneofwolvesbilly.orca.referencecore.application.ClientOperation;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JdbcClientDiagnosticRecordRepositoryTest {

    private static final ClientFailureReferenceId REFERENCE_ID =
            ClientFailureReferenceId.of("7f1eb30a-86d0-4a3e-89c8-a6ff395ec144");
    private static final Instant OCCURRED_AT = Instant.parse("2026-07-02T00:00:00Z");

    private JdbcClientDiagnosticRecordRepository repository;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DataSource dataSource = newDataSource();
        migrate(dataSource);
        repository = new JdbcClientDiagnosticRecordRepository(dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void persists_and_loads_allowlisted_client_diagnostic_fields() {
        ClientDiagnosticRecord record = ClientDiagnosticRecord.create(
                REFERENCE_ID,
                OCCURRED_AT,
                ClientDiagnosticCategory.MALFORMED_RESPONSE,
                ClientOperation.PASSWORD_LOGIN,
                ClientApplication.REACT,
                500
        );

        repository.save(record);

        assertEquals(record, repository.findByReferenceId(REFERENCE_ID).orElseThrow());
    }

    @Test
    void schema_has_no_generic_or_sensitive_payload_columns() {
        Integer forbiddenColumnCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'reference_core_client_diagnostics'
                  AND COLUMN_NAME IN (
                    'payload',
                    'metadata',
                    'password',
                    'request_body',
                    'response_body',
                    'stack_trace',
                    'exception_message',
                    'session_cookie',
                    'user_id',
                    'login_failure_reference_id'
                  )
                """,
                Integer.class
        );

        assertEquals(0, forbiddenColumnCount);
    }

    private static DataSource newDataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(
                "jdbc:h2:mem:orca_reference_core_client_diagnostics;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1"
        );
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
        return dataSource;
    }

    private static void migrate(DataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load()
                .clean();
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load()
                .migrate();
    }
}
