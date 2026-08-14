package io.github.oneofwolvesbilly.orca.organization.application;

import java.util.Objects;

/** Exposes typed organization failure meaning at the application boundary. */
public final class OrganizationApplicationFailure extends RuntimeException {

    private final OrganizationFailureCategory category;

    public OrganizationApplicationFailure(OrganizationFailureCategory category, String message) {
        super(message);
        this.category = Objects.requireNonNull(category, "category");
    }

    public OrganizationFailureCategory category() {
        return category;
    }
}
