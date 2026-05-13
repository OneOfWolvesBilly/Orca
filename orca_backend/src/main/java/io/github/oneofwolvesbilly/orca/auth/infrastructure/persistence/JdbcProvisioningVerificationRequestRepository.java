package io.github.oneofwolvesbilly.orca.auth.infrastructure.persistence;

import io.github.oneofwolvesbilly.orca.auth.application.ProvisioningVerificationRequestRepository;
import io.github.oneofwolvesbilly.orca.auth.domain.ProvisioningVerificationRequest;
import io.github.oneofwolvesbilly.orca.auth.domain.ProvisioningVerificationRequestId;
import io.github.oneofwolvesbilly.orca.auth.domain.VerificationCode;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.Optional;

public final class JdbcProvisioningVerificationRequestRepository
        implements ProvisioningVerificationRequestRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcProvisioningVerificationRequestRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(Objects.requireNonNull(dataSource, "dataSource"));
    }

    @Override
    public Optional<ProvisioningVerificationRequest> findById(ProvisioningVerificationRequestId id) {
        Objects.requireNonNull(id, "id");
        return jdbcTemplate.query(
                """
                SELECT verification_request_id, verification_code, expires_at, verified
                FROM auth_provisioning_verification_requests
                WHERE verification_request_id = ?
                """,
                (rs, rowNum) -> toRequest(rs),
                id.toString()
        ).stream().findFirst();
    }

    @Override
    public void save(ProvisioningVerificationRequest request) {
        Objects.requireNonNull(request, "request");
        jdbcTemplate.update(
                """
                UPDATE auth_provisioning_verification_requests
                SET verified = ?
                WHERE verification_request_id = ?
                """,
                request.isVerified(),
                request.id().toString()
        );
    }

    private static ProvisioningVerificationRequest toRequest(ResultSet rs) throws SQLException {
        ProvisioningVerificationRequestId id =
                ProvisioningVerificationRequestId.of(rs.getString("verification_request_id"));
        VerificationCode code = VerificationCode.of(rs.getString("verification_code"));
        Timestamp expiresAt = rs.getTimestamp("expires_at");
        boolean verified = rs.getBoolean("verified");

        if (verified) {
            return ProvisioningVerificationRequest.verified(id, code, expiresAt.toInstant());
        }
        return ProvisioningVerificationRequest.pending(id, code, expiresAt.toInstant());
    }
}
