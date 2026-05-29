package io.github.oneofwolvesbilly.orca.auth.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
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
        var context = currentUserContextResolver.resolve(sessionIdFrom(request));
        CurrentUserContextRequestAttribute.store(request, context);
        return true;
    }

    private static String sessionIdFrom(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(cookie -> PasswordLoginController.SESSION_COOKIE_NAME.equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }
}
