package io.github.oneofwolvesbilly.orca.auth.web;

/** Raised when HTTP current user context cannot be established for a protected request. */
public final class UnauthenticatedHttpRequestException extends RuntimeException {

    public UnauthenticatedHttpRequestException() {
        super("Authenticated user is required");
    }
}
