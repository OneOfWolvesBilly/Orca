package io.github.oneofwolvesbilly.orca.auth.application;

import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedUserId;
import io.github.oneofwolvesbilly.orca.auth.domain.LoginIdentifier;
import io.github.oneofwolvesbilly.orca.auth.domain.SubmittedPassword;

public interface LoginCredentialVerifier {

    AuthenticatedUserId verify(LoginIdentifier loginIdentifier, SubmittedPassword password);
}
