CREATE TABLE auth_provisioning_verification_requests (
    verification_request_id VARCHAR(36) PRIMARY KEY,
    verification_code VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    verified BOOLEAN NOT NULL
);
