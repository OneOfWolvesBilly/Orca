package io.github.oneofwolvesbilly.orca.auth.application;

public final class UnauthorizedAuthOperationException extends RuntimeException {

    public UnauthorizedAuthOperationException() {
        super("Authenticated user is not authorized for this auth operation");
    }
}
