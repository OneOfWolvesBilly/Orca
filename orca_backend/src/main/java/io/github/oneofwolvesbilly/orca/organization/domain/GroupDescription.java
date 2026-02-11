package io.github.oneofwolvesbilly.orca.organization.domain;

public record GroupDescription(String value) {
    public static GroupDescription ofNullable(String value) {
        return value == null ? null : new GroupDescription(value);
    }
}
