package io.github.oneofwolvesbilly.orca.auth.application;

public record PasswordLoginCommand(String loginIdentifier, String password) {
}
