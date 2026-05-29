CREATE TABLE auth_login_credentials (
    login_identifier VARCHAR(255) PRIMARY KEY,
    password_hash VARCHAR(64) NOT NULL,
    user_id VARCHAR(255) NOT NULL
);

CREATE TABLE auth_authenticated_sessions (
    session_id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_auth_authenticated_sessions_user
        FOREIGN KEY (user_id) REFERENCES auth_registered_users(user_id)
);
