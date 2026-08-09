package io.github.oneofwolvesbilly.orca.auth.infrastructure.spring;

import io.github.oneofwolvesbilly.orca.auth.api.OrcaProtectedCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddedProtectedCommandStartupValidatorTest {

    @Test
    void protected_declaration_without_embedded_enablement_fails_startup() {
        var validator = validator(false, mapping(RequestMethod.POST, "protectedCommand"));

        var failure = assertThrows(
                EmbeddedProtectedCommandConfigurationException.class,
                validator::afterSingletonsInstantiated
        );

        assertTrue(failure.getMessage().contains("@EnableOrcaEmbeddedAuth"));
    }

    @ParameterizedTest
    @EnumSource(value = RequestMethod.class, names = {"POST", "PUT", "PATCH", "DELETE"})
    void supported_protected_command_methods_pass_startup_validation(RequestMethod method) {
        var validator = validator(true, mapping(method, "protectedCommand"));

        assertDoesNotThrow(validator::afterSingletonsInstantiated);
    }

    @ParameterizedTest
    @EnumSource(
            value = RequestMethod.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = {"POST", "PUT", "PATCH", "DELETE"}
    )
    void unsupported_protected_methods_fail_startup(RequestMethod method) {
        var validator = validator(true, mapping(method, "protectedCommand"));

        var failure = assertThrows(
                EmbeddedProtectedCommandConfigurationException.class,
                validator::afterSingletonsInstantiated
        );

        assertTrue(failure.getMessage().contains(method.name()));
    }

    @Test
    void unspecified_protected_method_fails_startup() {
        var validator = validator(true, mapping(new RequestMethod[0], "protectedCommand"));

        assertThrows(
                EmbeddedProtectedCommandConfigurationException.class,
                validator::afterSingletonsInstantiated
        );
    }

    @Test
    void mixed_supported_and_unsupported_methods_fail_startup() {
        var validator = validator(true, mapping(
                new RequestMethod[]{RequestMethod.POST, RequestMethod.GET},
                "protectedCommand"
        ));

        assertThrows(
                EmbeddedProtectedCommandConfigurationException.class,
                validator::afterSingletonsInstantiated
        );
    }

    @Test
    void unprotected_handler_does_not_require_embedded_enablement() {
        var validator = validator(false, mapping(RequestMethod.GET, "unprotectedCommand"));

        assertDoesNotThrow(validator::afterSingletonsInstantiated);
    }

    @Test
    void type_level_protected_declaration_is_validated() {
        var validator = validator(false, typeProtectedMapping(RequestMethod.POST));

        assertThrows(
                EmbeddedProtectedCommandConfigurationException.class,
                validator::afterSingletonsInstantiated
        );
    }

    private static EmbeddedProtectedCommandStartupValidator validator(
            boolean enabled,
            Map<RequestMappingInfo, HandlerMethod> mappings
    ) {
        return new EmbeddedProtectedCommandStartupValidator(() -> mappings, () -> enabled);
    }

    private static Map<RequestMappingInfo, HandlerMethod> mapping(RequestMethod method, String handlerName) {
        return mapping(new RequestMethod[]{method}, handlerName);
    }

    private static Map<RequestMappingInfo, HandlerMethod> mapping(RequestMethod[] methods, String handlerName) {
        try {
            Method method = TestController.class.getDeclaredMethod(handlerName);
            RequestMappingInfo mapping = RequestMappingInfo.paths("/test")
                    .methods(methods)
                    .build();
            return Map.of(mapping, new HandlerMethod(new TestController(), method));
        } catch (NoSuchMethodException ex) {
            throw new AssertionError(ex);
        }
    }

    private static Map<RequestMappingInfo, HandlerMethod> typeProtectedMapping(RequestMethod method) {
        try {
            Method handler = TypeProtectedController.class.getDeclaredMethod("command");
            RequestMappingInfo mapping = RequestMappingInfo.paths("/type-protected")
                    .methods(method)
                    .build();
            return Map.of(mapping, new HandlerMethod(new TypeProtectedController(), handler));
        } catch (NoSuchMethodException ex) {
            throw new AssertionError(ex);
        }
    }

    private static final class TestController {

        @OrcaProtectedCommand
        void protectedCommand() {
        }

        void unprotectedCommand() {
        }
    }

    @OrcaProtectedCommand
    private static final class TypeProtectedController {

        void command() {
        }
    }
}
