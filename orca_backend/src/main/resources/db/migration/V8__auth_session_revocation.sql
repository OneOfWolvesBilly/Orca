ALTER TABLE auth_authenticated_sessions
    ADD COLUMN revoked_at TIMESTAMP NULL;
