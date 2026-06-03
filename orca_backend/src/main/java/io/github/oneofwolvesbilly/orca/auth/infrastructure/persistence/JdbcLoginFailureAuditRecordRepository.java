package io.github.oneofwolvesbilly.orca.auth.infrastructure.persistence;

import io.github.oneofwolvesbilly.orca.auth.application.LoginFailureAuditRecordRepository;
import io.github.oneofwolvesbilly.orca.auth.domain.LoginFailureAuditRecord;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.Objects;

public final class JdbcLoginFailureAuditRecordRepository implements LoginFailureAuditRecordRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcLoginFailureAuditRecordRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(Objects.requireNonNull(dataSource, "dataSource"));
    }

    @Override
    public void save(LoginFailureAuditRecord record) {
        Objects.requireNonNull(record, "record");
        jdbcTemplate.update(
                """
                INSERT INTO auth_login_failure_audits (
                    reference_id,
                    occurred_at,
                    submitted_login_identifier,
                    reason
                )
                VALUES (?, ?, ?, ?)
                """,
                record.referenceId().value(),
                Timestamp.from(record.occurredAt()),
                record.submittedLoginIdentifier(),
                record.reason().name()
        );
    }
}
