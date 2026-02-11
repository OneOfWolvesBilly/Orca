package io.github.oneofwolvesbilly.orca.organization.domain;

public record UserId(String value) {
    public static UserId of(String value) {
        return new UserId(value);
    }
}
