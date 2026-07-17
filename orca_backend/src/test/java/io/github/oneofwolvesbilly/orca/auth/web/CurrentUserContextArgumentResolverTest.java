package io.github.oneofwolvesbilly.orca.auth.web;

import io.github.oneofwolvesbilly.orca.auth.api.AuthenticatedActor;
import io.github.oneofwolvesbilly.orca.auth.domain.AuthenticatedUserId;
import io.github.oneofwolvesbilly.orca.auth.domain.CurrentUserContext;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUserContextArgumentResolverTest {

    private final CurrentUserContextArgumentResolver resolver = new CurrentUserContextArgumentResolver();

    @Test
    void supports_parameter_for_current_user_context() throws NoSuchMethodException {
        assertTrue(resolver.supportsParameter(parameter("handle", CurrentUserContext.class)));
    }

    @Test
    void supports_public_authenticated_actor_parameter() throws NoSuchMethodException {
        assertTrue(resolver.supportsParameter(parameter("handleActor", AuthenticatedActor.class)));
    }

    @Test
    void does_not_support_other_parameter_types() throws NoSuchMethodException {
        assertFalse(resolver.supportsParameter(parameter("ignore", String.class)));
    }

    @Test
    void resolve_argument_returns_request_scoped_current_user_context() throws Exception {
        CurrentUserContext context = CurrentUserContext.establish(AuthenticatedUserId.of("user-1"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(CurrentUserContextRequestAttribute.ATTRIBUTE_NAME, context);

        Object resolved = resolver.resolveArgument(
                parameter("handle", CurrentUserContext.class),
                new ModelAndViewContainer(),
                new ServletWebRequest(request, new MockHttpServletResponse()),
                null
        );

        assertSame(context, resolved);
    }

    @Test
    void maps_internal_context_to_public_authenticated_actor() throws Exception {
        CurrentUserContext context = CurrentUserContext.establish(AuthenticatedUserId.of("user-1"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(CurrentUserContextRequestAttribute.ATTRIBUTE_NAME, context);

        Object resolved = resolver.resolveArgument(
                parameter("handleActor", AuthenticatedActor.class),
                new ModelAndViewContainer(),
                new ServletWebRequest(request, new MockHttpServletResponse()),
                null
        );

        assertSame(AuthenticatedActor.class, resolved.getClass());
        assertTrue(resolved instanceof AuthenticatedActor actor && actor.actorId().equals("user-1"));
    }

    @Test
    void resolve_argument_rejects_when_request_has_no_established_current_user_context() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThrows(IllegalStateException.class, () ->
                resolver.resolveArgument(
                        parameter("handle", CurrentUserContext.class),
                        new ModelAndViewContainer(),
                        new ServletWebRequest(request, new MockHttpServletResponse()),
                        null
                )
        );
    }

    private static MethodParameter parameter(String methodName, Class<?> parameterType) throws NoSuchMethodException {
        Method method = ResolverTarget.class.getDeclaredMethod(methodName, parameterType);
        return new MethodParameter(method, 0);
    }

    private static final class ResolverTarget {

        @SuppressWarnings("unused")
        void handle(CurrentUserContext currentUserContext) {
        }

        @SuppressWarnings("unused")
        void handleActor(AuthenticatedActor authenticatedActor) {
        }

        @SuppressWarnings("unused")
        void ignore(String value) {
        }
    }
}
