CREATE TABLE reference_core_client_diagnostics (
    client_failure_reference_id VARCHAR(36) PRIMARY KEY,
    occurred_at TIMESTAMP(6) NOT NULL,
    category VARCHAR(64) NOT NULL,
    operation VARCHAR(64) NOT NULL,
    client_application VARCHAR(32) NOT NULL,
    response_status INTEGER NULL
);
