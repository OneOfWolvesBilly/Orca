CREATE TABLE organization_groups (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1024)
);

CREATE TABLE group_members (
    group_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    role VARCHAR(32) NOT NULL,
    PRIMARY KEY (group_id, user_id),
    CONSTRAINT fk_group_members_group
        FOREIGN KEY (group_id) REFERENCES organization_groups (id)
        ON DELETE CASCADE
);

CREATE TABLE group_invitations (
    id VARCHAR(64) PRIMARY KEY,
    group_id VARCHAR(64) NOT NULL,
    invitee_id VARCHAR(64) NOT NULL,
    intended_role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    CONSTRAINT fk_group_invitations_group
        FOREIGN KEY (group_id) REFERENCES organization_groups (id)
        ON DELETE CASCADE
);

CREATE TABLE invitation_index (
    invitation_id VARCHAR(64) PRIMARY KEY,
    group_id VARCHAR(64) NOT NULL,
    CONSTRAINT fk_invitation_index_invitation
        FOREIGN KEY (invitation_id) REFERENCES group_invitations (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_invitation_index_group
        FOREIGN KEY (group_id) REFERENCES organization_groups (id)
        ON DELETE CASCADE
);
