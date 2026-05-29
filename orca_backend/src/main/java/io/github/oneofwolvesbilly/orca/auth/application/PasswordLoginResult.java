package io.github.oneofwolvesbilly.orca.auth.application;

import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedSessionId;

import java.time.Instant;

public record PasswordLoginResult(AuthenticatedSessionId sessionId, Instant expiresAt) {
}
