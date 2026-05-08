package io.github.oneofwolvesbilly.orca.auth.web;

import io.github.oneofwolvesbilly.orca.auth.application.EstablishCurrentUserContextUseCase;
import io.github.oneofwolvesbilly.orca.auth.domain.CurrentUserContext;
import io.github.oneofwolvesbilly.orca.auth.support.FakeRegisteredUserIdentityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUserContextInterceptorTest {

    private static final FakeRegisteredUserIdentityRepository repository =
            new FakeRegisteredUserIdentityRepository().register("user-1");

    private final CurrentUserContextInterceptor interceptor =
            new CurrentUserContextInterceptor(new CurrentUserContextResolver(new EstablishCurrentUserContextUseCase(repository)));

    @Test
    void pre_handle_establishes_and_stores_current_user_context_when_one_header_is_presented() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.addHeader("X-User-Id", "user-1");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));

        CurrentUserContext context = (CurrentUserContext) request.getAttribute(
                CurrentUserContextRequestAttribute.ATTRIBUTE_NAME
        );
        assertEquals("user-1", context.authenticatedUserId().value());
    }

    @Test
    void pre_handle_rejects_when_no_header_is_presented() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");

        assertThrows(UnauthenticatedHttpRequestException.class, () ->
                interceptor.preHandle(request, new MockHttpServletResponse(), new Object())
        );
    }

    @Test
    void pre_handle_rejects_when_blank_header_is_presented() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.addHeader("X-User-Id", "   ");

        assertThrows(UnauthenticatedHttpRequestException.class, () ->
                interceptor.preHandle(request, new MockHttpServletResponse(), new Object())
        );
    }

    @Test
    void pre_handle_rejects_when_multiple_headers_are_presented() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.addHeader("X-User-Id", "user-1");
        request.addHeader("X-User-Id", "user-2");

        assertThrows(UnauthenticatedHttpRequestException.class, () ->
                interceptor.preHandle(request, new MockHttpServletResponse(), new Object())
        );
    }

    @Test
    void pre_handle_rejects_when_header_user_is_not_registered() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.addHeader("X-User-Id", "missing-user");

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
