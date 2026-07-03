package io.github.oneofwolvesbilly.orca.referencecore.infrastructure.persistence;

import io.github.oneofwolvesbilly.orca.referencecore.application.ClientApplication;
import io.github.oneofwolvesbilly.orca.referencecore.application.ClientDiagnosticCategory;
import io.github.oneofwolvesbilly.orca.referencecore.application.ClientDiagnosticRecord;
import io.github.oneofwolvesbilly.orca.referencecore.application.ClientDiagnosticRecordRepository;
import io.github.oneofwolvesbilly.orca.referencecore.application.ClientFailureReferenceId;
import io.github.oneofwolvesbilly.orca.referencecore.application.ClientOperation;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.Optional;

public final class JdbcClientDiagnosticRecordRepository implements ClientDiagnosticRecordRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcClientDiagnosticRecordRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(Objects.requireNonNull(dataSource, "dataSource"));
    }

    @Override
    public void save(ClientDiagnosticRecord record) {
        Objects.requireNonNull(record, "record");
        jdbcTemplate.update(
                """
                INSERT INTO reference_core_client_diagnostics (
                    client_failure_reference_id,
                    occurred_at,
                    category,
                    operation,
                    client_application,
                    response_status
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                record.referenceId().value(),
                Timestamp.from(record.occurredAt()),
                record.category().name(),
                record.operation().name(),
                record.clientApplication().name(),
                record.responseStatus()
        );
    }

    @Override
    public Optional<ClientDiagnosticRecord> findByReferenceId(ClientFailureReferenceId referenceId) {
        Objects.requireNonNull(referenceId, "referenceId");
        return jdbcTemplate.query(
                """
                SELECT occurred_at, category, operation, client_application, response_status
                FROM reference_core_client_diagnostics
                WHERE client_failure_reference_id = ?
                """,
                (resultSet, rowNumber) -> ClientDiagnosticRecord.create(
                        referenceId,
                        resultSet.getTimestamp("occurred_at").toInstant(),
                        ClientDiagnosticCategory.valueOf(resultSet.getString("category")),
                        ClientOperation.valueOf(resultSet.getString("operation")),
                        ClientApplication.valueOf(resultSet.getString("client_application")),
                        resultSet.getObject("response_status", Integer.class)
                ),
                referenceId.value()
        ).stream().findFirst();
    }
}
