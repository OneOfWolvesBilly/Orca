package io.github.oneofwolvesbilly.orca.organization.domain;

public record GroupName(String value) {
    public static GroupName of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("group name required");
        }
        return new GroupName(value);
    }
}