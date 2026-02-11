package io.github.oneofwolvesbilly.orca.organization.application;

import io.github.oneofwolvesbilly.orca.organization.domain.UserId;

public record CreateGroupCommand(UserId creatorUserId, String name, String description) {
}
