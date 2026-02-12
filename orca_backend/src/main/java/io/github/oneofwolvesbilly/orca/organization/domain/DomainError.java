package io.github.oneofwolvesbilly.orca.organization.domain;


/**
 * Enumerates domain-level error categories.
 * These are stable identifiers for domain failures (message/UI mapping is out of scope here).
 */
public enum DomainError {
    USER_ID_EMPTY,
    INVITER_NOT_GROUP_ADMIN,
    DUPLICATE_PENDING_INVITATION,
    INVITEE_ALREADY_MEMBER
}