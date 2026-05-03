package io.github.oneofwolvesbilly.orca.auth.application;

/** Raised when protected behavior is invoked without an authenticated user. */
public final class UnauthenticatedOperationException extends RuntimeException {

    public UnauthenticatedOperationException() {
        super("Authenticated user is required");
    }
}
