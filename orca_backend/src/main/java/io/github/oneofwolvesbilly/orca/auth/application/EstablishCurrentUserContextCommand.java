package io.github.oneofwolvesbilly.orca.auth.application;

import java.util.List;
import java.util.Objects;

/** Carries the authenticated identities presented for one protected operation. */
public record EstablishCurrentUserContextCommand(List<String> presentedAuthenticatedUserIds) {

    public EstablishCurrentUserContextCommand {
        Objects.requireNonNull(presentedAuthenticatedUserIds, "presentedAuthenticatedUserIds");
        presentedAuthenticatedUserIds = List.copyOf(presentedAuthenticatedUserIds);
    }
}
