package io.github.oneofwolvesbilly.orca.auth.application;

public final class RegisteredUserIdentityAlreadyExistsException extends RuntimeException {

    public RegisteredUserIdentityAlreadyExistsException() {
        super("Registered user identity already exists");
    }
}
