CREATE TABLE auth_login_failure_audits (
    reference_id VARCHAR(64) PRIMARY KEY,
    occurred_at TIMESTAMP NOT NULL,
    submitted_login_identifier VARCHAR(255),
    reason VARCHAR(64) NOT NULL
);
