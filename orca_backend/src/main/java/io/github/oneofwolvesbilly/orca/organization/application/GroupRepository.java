package io.github.oneofwolvesbilly.orca.organization.application;

import io.github.oneofwolvesbilly.orca.organization.domain.Group;

/** Persists Group aggregates. */
public interface GroupRepository {
    void save(Group group);
}