package io.github.oneofwolvesbilly.orca.auth.application;

import io.github.oneofwolvesbilly.orca.auth.domain.LoginFailureReferenceId;

/** One indistinguishable failure category for rejected password login attempts. */
public final class LoginRejectedException extends RuntimeException {

    private final LoginFailureReferenceId loginFailureReferenceId;

    public LoginRejectedException() {
        this.loginFailureReferenceId = null;
    }

    public LoginRejectedException(LoginFailureReferenceId loginFailureReferenceId) {
        this.loginFailureReferenceId = loginFailureReferenceId;
    }

    public LoginFailureReferenceId loginFailureReferenceId() {
        return loginFailureReferenceId;
    }
}
