package io.github.oneofwolvesbilly.orca.auth.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Collections;
import java.util.Objects;

/** Establishes request-scoped current user context for protected HTTP requests. */
public final class CurrentUserContextInterceptor implements HandlerInterceptor {

    private final CurrentUserContextResolver currentUserContextResolver;

    public CurrentUserContextInterceptor(CurrentUserContextResolver currentUserContextResolver) {
        this.currentUserContextResolver = Objects.requireNonNull(currentUserContextResolver, "currentUserContextResolver");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"POST".equals(request.getMethod())) {
            return true;
        }
        var context = currentUserContextResolver.resolve(
                Collections.list(request.getHeaders("X-User-Id"))
        );
        CurrentUserContextRequestAttribute.store(request, context);
        return true;
    }
}
