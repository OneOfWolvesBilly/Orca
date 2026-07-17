package io.github.oneofwolvesbilly.orca.auth.web;

import io.github.oneofwolvesbilly.orca.auth.api.OrcaProtectedCommand;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Establishes request-scoped current user context for protected HTTP requests. */
public final class CurrentUserContextInterceptor implements HandlerInterceptor {

    private final CurrentUserContextResolver currentUserContextResolver;

    public CurrentUserContextInterceptor(CurrentUserContextResolver currentUserContextResolver) {
        this.currentUserContextResolver = Objects.requireNonNull(currentUserContextResolver, "currentUserContextResolver");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"POST".equals(request.getMethod()) || !isProtected(handler)) {
            return true;
        }
        var context = currentUserContextResolver.resolve(sessionIdFrom(request));
        CurrentUserContextRequestAttribute.store(request, context);
        return true;
    }

    private static String sessionIdFrom(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        List<String> sessionIds = Arrays.stream(cookies)
                .filter(cookie -> PasswordLoginController.SESSION_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .toList();
        if (sessionIds.size() > 1) {
            throw new UnauthenticatedHttpRequestException();
        }
        return sessionIds.isEmpty() ? null : sessionIds.getFirst();
    }

    private static boolean isProtected(Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return false;
        }
        return AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), OrcaProtectedCommand.class)
                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), OrcaProtectedCommand.class);
    }
}
