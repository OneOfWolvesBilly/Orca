package io.github.oneofwolvesbilly.orca.organization.application;

import io.github.oneofwolvesbilly.orca.organization.domain.GroupId;

public interface GroupIdGenerator {
    GroupId nextId();
}