package io.github.oneofwolvesbilly.orca.auth.domain;

/** Server-side-only troubleshooting category for rejected password login attempts. */
public enum LoginFailureReason {
    INVALID_INPUT,
    INVALID_CREDENTIALS
}
