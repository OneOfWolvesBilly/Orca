package io.github.oneofwolvesbilly.orca.organization.infrastructure.inmemory;

import io.github.oneofwolvesbilly.orca.organization.application.GroupRepository;
import io.github.oneofwolvesbilly.orca.organization.domain.Group;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Stores groups in memory for tests and local runs. */
public final class InMemoryGroupRepository implements GroupRepository {

    private final List<Group> savedGroups = new ArrayList<>();

    @Override
    public void save(Group group) {
        savedGroups.add(Objects.requireNonNull(group, "group"));
    }

    /** Returns saved groups for assertions. */
    public List<Group> savedGroups() {
        return Collections.unmodifiableList(savedGroups);
    }
}