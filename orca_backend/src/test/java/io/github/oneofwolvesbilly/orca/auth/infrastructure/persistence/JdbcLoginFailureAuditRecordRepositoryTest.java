package io.github.oneofwolvesbilly.orca.auth.infrastructure.persistence;

import io.github.oneofwolvesbilly.orca.auth.application.LoginFailureAuditRecordRepository;
import io.github.oneofwolvesbilly.orca.auth.domain.LoginFailureAuditRecord;
import io.github.oneofwolvesbilly.orca.auth.domain.LoginFailureReason;
import io.github.oneofwolvesbilly.orca.auth.domain.LoginFailureReferenceId;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JdbcLoginFailureAuditRecordRepositoryTest {

    private static final LoginFailureReferenceId REFERENCE_ID =
            LoginFailureReferenceId.of("7f1eb30a-86d0-4a3e-89c8-a6ff395ec144");
    private static final Instant OCCURRED_AT = Instant.parse("2026-06-03T00:00:00Z");

    private LoginFailureAuditRecordRepository repository;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DataSource dataSource = newDataSource();
        migrate(dataSource);
        repository = new JdbcLoginFailureAuditRecordRepository(dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void save_persists_server_side_login_failure_audit_state() {
        repository.save(LoginFailureAuditRecord.create(
                REFERENCE_ID,
                OCCURRED_AT,
                "employee-login-001",
                LoginFailureReason.INVALID_CREDENTIALS
        ));

        String submittedLoginIdentifier = jdbcTemplate.queryForObject(
                "SELECT submitted_login_identifier FROM auth_login_failure_audits WHERE reference_id = ?",
                String.class,
                REFERENCE_ID.value()
        );
        String reason = jdbcTemplate.queryForObject(
                "SELECT reason FROM auth_login_failure_audits WHERE reference_id = ?",
                String.class,
                REFERENCE_ID.value()
        );
        Timestamp occurredAt = jdbcTemplate.queryForObject(
                "SELECT occurred_at FROM auth_login_failure_audits WHERE reference_id = ?",
                Timestamp.class,
                REFERENCE_ID.value()
        );

        assertEquals("employee-login-001", submittedLoginIdentifier);
        assertEquals("INVALID_CREDENTIALS", reason);
        assertEquals(OCCURRED_AT, occurredAt.toInstant());
    }

    @Test
    void save_does_not_persist_password_or_session_cookie_columns() {
        repository.save(LoginFailureAuditRecord.create(
                REFERENCE_ID,
                OCCURRED_AT,
                null,
                LoginFailureReason.INVALID_INPUT
        ));

        Integer forbiddenColumnCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'auth_login_failure_audits'
                  AND COLUMN_NAME IN ('password', 'submitted_password', 'session_cookie', 'raw_session_cookie')
                """,
                Integer.class
        );

        assertEquals(0, forbiddenColumnCount);
    }

    private static DataSource newDataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:orca_auth_login_failure_audits;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
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
