package io.github.oneofwolvesbilly.orca.auth.application;

/** Raised when more than one authenticated identity is presented for one operation. */
public final class AmbiguousAuthenticatedUserException extends RuntimeException {

    public AmbiguousAuthenticatedUserException() {
        super("Exactly one authenticated user is required");
    }
}
