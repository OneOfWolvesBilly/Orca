package io.github.oneofwolvesbilly.orca.organization.domain;

public record GroupId(String value) {
    public static GroupId of(String value) {
        return new GroupId(value);
    }
}
