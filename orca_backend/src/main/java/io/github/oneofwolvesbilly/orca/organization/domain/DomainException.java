package io.github.oneofwolvesbilly.orca.organization.domain;

/**
 * Represents a domain rule violation with a machine-checkable error category.
 */
public final class DomainException extends RuntimeException {

    private final DomainError error;

    public DomainException(DomainError error, String message) {
        super(message);
        this.error = error;
    }

    public DomainError error() {
        return error;
    }
}