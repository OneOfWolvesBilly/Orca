package io.github.oneofwolvesbilly.orca.auth.infrastructure.persistence;

import io.github.oneofwolvesbilly.orca.auth.application.ProvisioningVerificationRequestRepository;
import io.github.oneofwolvesbilly.orca.auth.domain.ProvisioningVerificationRequest;
import io.github.oneofwolvesbilly.orca.auth.domain.ProvisioningVerificationRequestId;
import io.github.oneofwolvesbilly.orca.auth.domain.VerificationCode;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcProvisioningVerificationRequestRepositoryTest {

    private static final String REQUEST_ID = "3f1eb30a-86d0-4a3e-89c8-a6ff395ec144";
    private static final Instant EXPIRES_AT = Instant.parse("2026-05-13T00:05:00Z");

    private ProvisioningVerificationRequestRepository repository;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DataSource dataSource = newDataSource();
        migrate(dataSource);
        repository = new JdbcProvisioningVerificationRequestRepository(dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void find_by_id_loads_pending_request_state() {
        insertRequest(REQUEST_ID, "123456", EXPIRES_AT, false);

        ProvisioningVerificationRequest request = repository
                .findById(ProvisioningVerificationRequestId.of(REQUEST_ID))
                .orElseThrow();

        assertEquals(ProvisioningVerificationRequestId.of(REQUEST_ID), request.id());
        assertEquals(VerificationCode.of("123456"), request.verificationCode());
        assertEquals(EXPIRES_AT, request.expiresAt());
        assertFalse(request.isVerified());
    }

    @Test
    void find_by_id_returns_empty_for_unknown_request() {
        assertTrue(repository.findById(ProvisioningVerificationRequestId.of(REQUEST_ID)).isEmpty());
    }

    @Test
    void save_persists_verified_state() {
        insertRequest(REQUEST_ID, "123456", EXPIRES_AT, false);
        ProvisioningVerificationRequest request = repository
                .findById(ProvisioningVerificationRequestId.of(REQUEST_ID))
                .orElseThrow();

        request.confirm(VerificationCode.of("123456"), Instant.parse("2026-05-13T00:00:00Z"));
        repository.save(request);

        Boolean verified = jdbcTemplate.queryForObject(
                "SELECT verified FROM auth_provisioning_verification_requests WHERE verification_request_id = ?",
                Boolean.class,
                REQUEST_ID
        );
        assertEquals(Boolean.TRUE, verified);
    }

    private void insertRequest(String requestId, String code, Instant expiresAt, boolean verified) {
        jdbcTemplate.update(
                """
                INSERT INTO auth_provisioning_verification_requests
                    (verification_request_id, verification_code, expires_at, verified)
                VALUES (?, ?, ?, ?)
                """,
                requestId,
                code,
                Timestamp.from(expiresAt),
                verified
        );
    }

    private static DataSource newDataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:orca_auth_provisioning_verification;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
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
