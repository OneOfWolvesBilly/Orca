package io.github.oneofwolvesbilly.orca.auth.api;

import io.github.oneofwolvesbilly.orca.auth.infrastructure.spring.EmbeddedAuthImportSelector;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Enables Orca login, logout, and protected actor resolution in an embedded Spring host. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(EmbeddedAuthImportSelector.class)
public @interface EnableOrcaEmbeddedAuth {
}
