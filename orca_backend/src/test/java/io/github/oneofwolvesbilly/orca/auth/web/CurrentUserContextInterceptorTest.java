package io.github.oneofwolvesbilly.orca.auth.web;

import io.github.oneofwolvesbilly.orca.auth.application.EstablishCurrentUserContextUseCase;
import io.github.oneofwolvesbilly.orca.auth.application.ResolveCurrentUserContextFromSessionUseCase;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedSession;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedSessionId;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedUserId;
import io.github.oneofwolvesbilly.orca.auth.domain.CurrentUserContext;
import io.github.oneofwolvesbilly.orca.auth.support.FakeAuthenticatedSessionRepository;
import io.github.oneofwolvesbilly.orca.auth.support.FakeRegisteredUserIdentityRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUserContextInterceptorTest {

    private static final Instant NOW = Instant.parse("2026-05-29T00:00:00Z");
    private static final AuthenticatedSessionId SESSION_ID =
            AuthenticatedSessionId.of("3f1eb30a-86d0-4a3e-89c8-a6ff395ec144");
    private static final FakeRegisteredUserIdentityRepository repository =
            new FakeRegisteredUserIdentityRepository().register("user-1");

    private static final FakeAuthenticatedSessionRepository sessions = new FakeAuthenticatedSessionRepository();

    static {
        sessions.save(AuthenticatedSession.create(
                SESSION_ID,
                AuthenticatedUserId.of("user-1"),
                NOW.minusSeconds(60),
                NOW.plusSeconds(60)
        ));
    }

    private final CurrentUserContextInterceptor interceptor =
            new CurrentUserContextInterceptor(new CurrentUserContextResolver(new ResolveCurrentUserContextFromSessionUseCase(
                    sessions,
                    new EstablishCurrentUserContextUseCase(repository),
                    Clock.fixed(NOW, ZoneOffset.UTC)
            )));

    @Test
    void pre_handle_establishes_and_stores_current_user_context_when_session_cookie_is_presented() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setCookies(new Cookie(PasswordLoginController.SESSION_COOKIE_NAME, SESSION_ID.value()));

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));

        CurrentUserContext context = (CurrentUserContext) request.getAttribute(
                CurrentUserContextRequestAttribute.ATTRIBUTE_NAME
        );
        assertEquals("user-1", context.authenticatedUserId().value());
    }

    @Test
    void pre_handle_rejects_when_no_session_cookie_is_presented() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");

        assertThrows(UnauthenticatedHttpRequestException.class, () ->
                interceptor.preHandle(request, new MockHttpServletResponse(), new Object())
        );
    }

    @Test
    void pre_handle_rejects_when_blank_session_cookie_is_presented() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setCookies(new Cookie(PasswordLoginController.SESSION_COOKIE_NAME, "   "));

        assertThrows(UnauthenticatedHttpRequestException.class, () ->
                interceptor.preHandle(request, new MockHttpServletResponse(), new Object())
        );
    }

    @Test
    void pre_handle_rejects_when_session_is_unknown() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setCookies(new Cookie(PasswordLoginController.SESSION_COOKIE_NAME, "missing-session"));

        assertThrows(UnauthenticatedHttpRequestException.class, () ->
                interceptor.preHandle(request, new MockHttpServletResponse(), new Object())
        );
    }

    @Test
    void pre_handle_rejects_when_only_x_user_id_is_presented() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.addHeader("X-User-Id", "user-1");

        assertThrows(UnauthenticatedHttpRequestException.class, () ->
                interceptor.preHandle(request, new MockHttpServletResponse(), new Object())
        );
    }

    @Test
    void pre_handle_does_not_require_current_user_context_for_non_post_requests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertNull(request.getAttribute(CurrentUserContextRequestAttribute.ATTRIBUTE_NAME));
    }
}
