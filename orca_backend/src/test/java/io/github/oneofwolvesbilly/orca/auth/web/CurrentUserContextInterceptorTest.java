package io.github.oneofwolvesbilly.orca.auth.web;

import io.github.oneofwolvesbilly.orca.auth.application.EstablishCurrentUserContextUseCase;
import io.github.oneofwolvesbilly.orca.auth.domain.CurrentUserContext;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUserContextInterceptorTest {

    private final CurrentUserContextInterceptor interceptor =
            new CurrentUserContextInterceptor(new CurrentUserContextResolver(new EstablishCurrentUserContextUseCase()));

    @Test
    void pre_handle_establishes_and_stores_current_user_context_when_one_header_is_presented() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
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

        assertThrows(UnauthenticatedHttpRequestException.class, () ->
                interceptor.preHandle(request, new MockHttpServletResponse(), new Object())
        );
    }

    @Test
    void pre_handle_rejects_when_blank_header_is_presented() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "   ");

        assertThrows(UnauthenticatedHttpRequestException.class, () ->
                interceptor.preHandle(request, new MockHttpServletResponse(), new Object())
        );
    }

    @Test
    void pre_handle_rejects_when_multiple_headers_are_presented() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "user-1");
        request.addHeader("X-User-Id", "user-2");

        assertThrows(UnauthenticatedHttpRequestException.class, () ->
                interceptor.preHandle(request, new MockHttpServletResponse(), new Object())
        );
    }
}
