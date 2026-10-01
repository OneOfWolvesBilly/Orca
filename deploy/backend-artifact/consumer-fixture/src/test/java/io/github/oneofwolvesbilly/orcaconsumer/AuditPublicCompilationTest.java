package io.github.oneofwolvesbilly.orcaconsumer;

import io.github.oneofwolvesbilly.orca.referencecore.application.AuditRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.tools.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AuditPublicCompilationTest {
    private static final String PREFIX = "io.github.oneofwolvesbilly.orca.referencecore.application.";
    private static final Set<String> API = Set.of("AuditActorId", "AuditEventType", "AuditOutcome",
            "AuditMetadataEntry", "AuditMetadata", "AuditRecord", "AuditRecorder");
    @TempDir Path directory;

    @Test
    void independent_compiler_requires_only_jdk_and_resolved_artifact() throws Exception {
        compile("""
                var event = new io.github.oneofwolvesbilly.orca.referencecore.application.AuditEventType("event");
                var actor = new io.github.oneofwolvesbilly.orca.referencecore.application.AuditActorId("actor");
                var outcome = new io.github.oneofwolvesbilly.orca.referencecore.application.AuditOutcome("result");
                var entry = new io.github.oneofwolvesbilly.orca.referencecore.application.AuditMetadataEntry("key", "value");
                var metadata = new io.github.oneofwolvesbilly.orca.referencecore.application.AuditMetadata(java.util.List.of(entry));
                var record = new io.github.oneofwolvesbilly.orca.referencecore.application.AuditRecord(
                    event, actor, java.time.Instant.EPOCH, outcome, null, null, null, metadata);
                io.github.oneofwolvesbilly.orca.referencecore.application.AuditRecorder recorder = value -> {};
                recorder.record(record);
                """, true);
    }

    @Test
    void malformed_signatures_fail_for_type_errors_after_positive_control() throws Exception {
        compile("var value = " + PREFIX + "AuditEventType.of(\"valid\");", true);
        for (String expression : List.of(
                "AuditEventType.of(42)", "AuditActorId.of(new Object())", "AuditOutcome.of(true)",
                "AuditMetadataEntry.of(\"key\", new byte[]{1})",
                "AuditMetadata.of(java.util.List.of(\"untyped-entry\"))",
                "AuditRecord.create(\"event\", null, null, null)",
                "AuditRecord.create(null, null, java.time.LocalDateTime.now(), null)",
                "AuditRecord.create(null, null, \"2026-09-30\", null)")) {
            compile("var value = " + PREFIX + expression + ";", false);
        }
    }

    @Test
    void every_public_signature_closes_over_jdk_and_seven_approved_types() throws Exception {
        for (String name : API) {
            Class<?> type = Class.forName(PREFIX + name);
            check(type.getGenericSuperclass());
            for (Type contract : type.getGenericInterfaces()) check(contract);
            for (Constructor<?> constructor : type.getConstructors()) {
                for (Type parameter : constructor.getGenericParameterTypes()) check(parameter);
                for (Type failure : constructor.getGenericExceptionTypes()) check(failure);
            }
            for (Method method : type.getMethods()) {
                check(method.getGenericReturnType());
                for (Type parameter : method.getGenericParameterTypes()) check(parameter);
                for (Type failure : method.getGenericExceptionTypes()) check(failure);
            }
            for (Field field : type.getFields()) check(field.getGenericType());
        }
    }

    private void compile(String body, boolean expected) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "JDK compiler is required; absence is not a negative-test success");
        Path artifact = Path.of(AuditRecord.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        assertTrue(Files.isRegularFile(artifact), "Independent compilation must use a JAR, not reactor classes");
        Path source = directory.resolve("Probe.java");
        Files.writeString(source, "class Probe { void run() { " + body + " } }");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var manager = compiler.getStandardFileManager(diagnostics, null, null)) {
            boolean result = compiler.getTask(null, manager, diagnostics,
                    List.of("--release", "21", "-proc:none", "-classpath", artifact.toString(), "-d", directory.toString()),
                    null, manager.getJavaFileObjects(source.toFile())).call();
            assertEquals(expected, result, diagnostics.getDiagnostics().toString());
            if (!expected) {
                assertTrue(diagnostics.getDiagnostics().stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR
                        && (d.getCode().contains("prob.found.req") || d.getCode().contains("cant.apply"))),
                        "Negative must fail on incompatible arguments: " + diagnostics.getDiagnostics());
            }
        }
    }

    private static void check(Type type) {
        if (type == null) return;
        if (type instanceof Class<?> cls) {
            if (cls.isArray()) { check(cls.getComponentType()); return; }
            assertTrue(cls.isPrimitive() || cls.getName().startsWith("java.")
                    || API.stream().anyMatch(name -> cls.getName().equals(PREFIX + name)), cls.getName());
        } else if (type instanceof ParameterizedType parameterized) {
            check(parameterized.getRawType());
            for (Type argument : parameterized.getActualTypeArguments()) check(argument);
        } else if (type instanceof TypeVariable<?> variable) {
            for (Type bound : variable.getBounds()) check(bound);
        } else if (type instanceof WildcardType wildcard) {
            for (Type bound : wildcard.getLowerBounds()) check(bound);
            for (Type bound : wildcard.getUpperBounds()) check(bound);
        } else {
            fail("Unverified public signature: " + type);
        }
    }
}
