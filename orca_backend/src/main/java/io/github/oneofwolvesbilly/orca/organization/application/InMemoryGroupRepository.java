package io.github.oneofwolvesbilly.orca.organization.application;

import io.github.oneofwolvesbilly.orca.organization.domain.Group;

import java.util.ArrayList;
import java.util.List;

public final class InMemoryGroupRepository implements GroupRepository {

    private final List<Group> stored = new ArrayList<>();

    @Override
    public void save(Group group) {
        stored.add(group);
    }

}
