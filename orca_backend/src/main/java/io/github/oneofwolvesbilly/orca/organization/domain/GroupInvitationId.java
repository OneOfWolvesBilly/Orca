package io.github.oneofwolvesbilly.orca.organization.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifies a GroupInvitation.
 */
public record GroupInvitationId(String value) {

    public GroupInvitationId {
        Objects.requireNonNull(value, "invitationId");
        if (value.isBlank()) {
            throw new IllegalArgumentException("invitationId must not be blank");
        }
    }

    public static GroupInvitationId newId() {
        return new GroupInvitationId(UUID.randomUUID().toString());
    }

    public static GroupInvitationId of(String value) {
        return new GroupInvitationId(value);
    }
}
