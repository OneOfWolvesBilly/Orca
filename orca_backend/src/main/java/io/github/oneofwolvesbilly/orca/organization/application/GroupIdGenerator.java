package io.github.oneofwolvesbilly.orca.organization.application;

import io.github.oneofwolvesbilly.orca.organization.domain.GroupId;

/** Generates new group identifiers. */
public interface GroupIdGenerator {
    GroupId nextId();
}