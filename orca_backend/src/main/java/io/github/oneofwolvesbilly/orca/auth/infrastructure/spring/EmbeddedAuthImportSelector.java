package io.github.oneofwolvesbilly.orca.auth.infrastructure.spring;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/** Internal Spring wiring selected by the public embedded auth entry point. */
public final class EmbeddedAuthImportSelector implements ImportSelector {

    private static final String[] EMBEDDED_AUTH_COMPONENTS = {
            "io.github.oneofwolvesbilly.orca.infrastructure.spring.OrcaPersistenceConfiguration",
            "io.github.oneofwolvesbilly.orca.auth.infrastructure.spring.AuthConfiguration",
            "io.github.oneofwolvesbilly.orca.auth.web.PasswordLoginController",
            "io.github.oneofwolvesbilly.orca.auth.web.LogoutSessionController",
            "io.github.oneofwolvesbilly.orca.referencecore.web.GlobalApiExceptionHandler"
    };

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return EMBEDDED_AUTH_COMPONENTS.clone();
    }
}
