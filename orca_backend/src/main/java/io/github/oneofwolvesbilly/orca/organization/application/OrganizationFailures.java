package io.github.oneofwolvesbilly.orca.organization.application;

import io.github.oneofwolvesbilly.orca.organization.domain.DomainError;
import io.github.oneofwolvesbilly.orca.organization.domain.DomainException;

final class OrganizationFailures {

    private OrganizationFailures() {
    }

    static OrganizationApplicationFailure notFound(String message) {
        return new OrganizationApplicationFailure(OrganizationFailureCategory.NOT_FOUND, message);
    }

    static OrganizationApplicationFailure rejected(String message) {
        return new OrganizationApplicationFailure(OrganizationFailureCategory.APPLICATION_REJECTED, message);
    }

    static OrganizationApplicationFailure from(DomainException failure) {
        return new OrganizationApplicationFailure(category(failure.error()), failure.getMessage());
    }

    private static OrganizationFailureCategory category(DomainError error) {
        return switch (error) {
            case INVITATION_NOT_FOUND -> OrganizationFailureCategory.NOT_FOUND;
            case INVITER_NOT_GROUP_ADMIN,
                    INVITATION_ACCEPTOR_MISMATCH,
                    INVITATION_REJECTOR_MISMATCH -> OrganizationFailureCategory.FORBIDDEN;
            case USER_ID_EMPTY,
                    DUPLICATE_PENDING_INVITATION,
                    INVITEE_ALREADY_MEMBER,
                    INVITATION_NOT_PENDING -> OrganizationFailureCategory.APPLICATION_REJECTED;
        };
    }
}
