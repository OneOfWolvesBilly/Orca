package io.github.oneofwolvesbilly.orca.auth.web;

import io.github.oneofwolvesbilly.orca.auth.domain.CurrentUserContext;
import jakarta.servlet.http.HttpServletRequest;

final class CurrentUserContextRequestAttribute {

    static final String ATTRIBUTE_NAME = CurrentUserContextRequestAttribute.class.getName() + ".currentUserContext";

    private CurrentUserContextRequestAttribute() {
    }

    static void store(HttpServletRequest request, CurrentUserContext currentUserContext) {
        request.setAttribute(ATTRIBUTE_NAME, currentUserContext);
    }

    static CurrentUserContext load(HttpServletRequest request) {
        Object value = request.getAttribute(ATTRIBUTE_NAME);
        if (value instanceof CurrentUserContext currentUserContext) {
            return currentUserContext;
        }
        throw new IllegalStateException("Current user context is required");
    }
}
