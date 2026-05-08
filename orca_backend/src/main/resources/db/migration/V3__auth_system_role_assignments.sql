CREATE TABLE auth_system_role_assignments (
    user_id VARCHAR(64) NOT NULL,
    role VARCHAR(64) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_auth_system_role_user
        FOREIGN KEY (user_id)
        REFERENCES auth_registered_users (user_id)
);
