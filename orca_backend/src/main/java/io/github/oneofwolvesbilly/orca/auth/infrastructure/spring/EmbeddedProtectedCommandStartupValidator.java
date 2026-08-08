package io.github.oneofwolvesbilly.orca.auth.infrastructure.spring;

import io.github.oneofwolvesbilly.orca.auth.api.OrcaProtectedCommand;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

final class EmbeddedProtectedCommandStartupValidator implements SmartInitializingSingleton {

    private static final Set<RequestMethod> SUPPORTED_METHODS =
            EnumSet.of(RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE);

    private final Supplier<Map<RequestMappingInfo, HandlerMethod>> handlerMethods;
    private final BooleanSupplier embeddedAuthEnabled;

    EmbeddedProtectedCommandStartupValidator(
            RequestMappingHandlerMapping handlerMapping,
            ListableBeanFactory beanFactory
    ) {
        this(
                Objects.requireNonNull(handlerMapping, "handlerMapping")::getHandlerMethods,
                () -> Objects.requireNonNull(beanFactory, "beanFactory")
                        .getBeanNamesForType(EmbeddedAuthBoundaryEnabled.class).length > 0
        );
    }

    EmbeddedProtectedCommandStartupValidator(
            Supplier<Map<RequestMappingInfo, HandlerMethod>> handlerMethods,
            BooleanSupplier embeddedAuthEnabled
    ) {
        this.handlerMethods = Objects.requireNonNull(handlerMethods, "handlerMethods");
        this.embeddedAuthEnabled = Objects.requireNonNull(embeddedAuthEnabled, "embeddedAuthEnabled");
    }

    @Override
    public void afterSingletonsInstantiated() {
        Map<RequestMappingInfo, HandlerMethod> protectedMappings = handlerMethods.get()
                .entrySet()
                .stream()
                .filter(entry -> isProtected(entry.getValue()))
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (protectedMappings.isEmpty()) {
            return;
        }
        if (!embeddedAuthEnabled.getAsBoolean()) {
            throw new EmbeddedProtectedCommandConfigurationException(
                    "@OrcaProtectedCommand requires @EnableOrcaEmbeddedAuth"
            );
        }
        protectedMappings.forEach(EmbeddedProtectedCommandStartupValidator::validateMethods);
    }

    private static void validateMethods(RequestMappingInfo mapping, HandlerMethod handler) {
        Set<RequestMethod> methods = mapping.getMethodsCondition().getMethods();
        if (methods.isEmpty()) {
            throw unsupported(handler, "UNSPECIFIED");
        }
        for (RequestMethod method : methods) {
            if (!SUPPORTED_METHODS.contains(method)) {
                throw unsupported(handler, method.name());
            }
        }
    }

    private static EmbeddedProtectedCommandConfigurationException unsupported(
            HandlerMethod handler,
            String method
    ) {
        return new EmbeddedProtectedCommandConfigurationException(
                "Unsupported @OrcaProtectedCommand HTTP method %s on %s#%s"
                        .formatted(method, handler.getBeanType().getName(), handler.getMethod().getName())
        );
    }

    private static boolean isProtected(HandlerMethod handler) {
        return AnnotatedElementUtils.hasAnnotation(handler.getMethod(), OrcaProtectedCommand.class)
                || AnnotatedElementUtils.hasAnnotation(handler.getBeanType(), OrcaProtectedCommand.class);
    }
}
