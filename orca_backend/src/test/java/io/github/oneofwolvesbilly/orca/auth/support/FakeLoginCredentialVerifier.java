package io.github.oneofwolvesbilly.orca.auth.support;

import io.github.oneofwolvesbilly.orca.auth.application.LoginCredentialVerifier;
import io.github.oneofwolvesbilly.orca.auth.application.LoginRejectedException;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedUserId;
import io.github.oneofwolvesbilly.orca.auth.domain.LoginIdentifier;
import io.github.oneofwolvesbilly.orca.auth.domain.SubmittedPassword;

import java.util.HashMap;
import java.util.Map;

public final class FakeLoginCredentialVerifier implements LoginCredentialVerifier {

    private final Map<String, AuthenticatedUserId> acceptedCredentials = new HashMap<>();

    public FakeLoginCredentialVerifier accept(String loginIdentifier, String password, AuthenticatedUserId userId) {
        acceptedCredentials.put(key(loginIdentifier, password), userId);
        return this;
    }

    @Override
    public AuthenticatedUserId verify(LoginIdentifier loginIdentifier, SubmittedPassword password) {
        AuthenticatedUserId userId = acceptedCredentials.get(key(loginIdentifier.value(), password.value()));
        if (userId == null) {
            throw new LoginRejectedException();
        }
        return userId;
    }

    private static String key(String loginIdentifier, String password) {
        return loginIdentifier + "\n" + password;
    }
}
