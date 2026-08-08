package io.github.oneofwolvesbilly.orca.auth.infrastructure.spring;

/** Raised before startup completes when a protected command declaration is unsafe. */
final class EmbeddedProtectedCommandConfigurationException extends IllegalStateException {

    EmbeddedProtectedCommandConfigurationException(String message) {
        super(message);
    }
}
