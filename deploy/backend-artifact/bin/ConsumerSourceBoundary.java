import com.sun.source.tree.*;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePathScanner;
import java.nio.file.*;
import java.util.*;
import javax.tools.*;

/** Parses Java syntax without resolving dependencies; compilation is a separate gate. */
class ConsumerSourceBoundary {
    private static final String ROOT = "io.github.oneofwolvesbilly.orca";
    private static final Set<String> ALLOWED = Set.of(
        ROOT + ".auth.api.AuthenticatedActor", ROOT + ".auth.api.EnableOrcaEmbeddedAuth",
        ROOT + ".auth.api.OrcaProtectedCommand",
        ROOT + ".referencecore.application.AuditActorId", ROOT + ".referencecore.application.AuditEventType",
        ROOT + ".referencecore.application.AuditOutcome", ROOT + ".referencecore.application.AuditMetadataEntry",
        ROOT + ".referencecore.application.AuditMetadata", ROOT + ".referencecore.application.AuditRecord",
        ROOT + ".referencecore.application.AuditRecorder");

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected consumer source directory");
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("A JDK is required for the source boundary");
        List<Path> sources;
        try (var paths = Files.walk(Path.of(args[0]))) {
            sources = paths.filter(p -> p.toString().endsWith(".java")).toList();
        }
        if (sources.isEmpty()) throw new IllegalStateException("Consumer Java source is required");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var files = compiler.getStandardFileManager(diagnostics, null, null)) {
            var task = (JavacTask) compiler.getTask(null, files, diagnostics, List.of("-proc:none"), null,
                    files.getJavaFileObjectsFromPaths(sources));
            for (var unit : task.parse()) {
                if (unit.getPackageName() != null && isOrca(unit.getPackageName().toString())) reject();
                new TreePathScanner<Void, Void>() {
                    @Override public Void visitImport(ImportTree tree, Void unused) {
                        String name = tree.getQualifiedIdentifier().toString();
                        if (isOrca(name)) {
                            if (name.endsWith(".*")) reject();
                            String owner = tree.isStatic() ? name.substring(0, name.lastIndexOf('.')) : name;
                            if (!ALLOWED.contains(owner)) reject();
                        }
                        return null;
                    }
                    @Override public Void visitMemberSelect(MemberSelectTree tree, Void unused) {
                        var parent = getCurrentPath().getParentPath().getLeaf();
                        boolean partOfLargerName = parent instanceof MemberSelectTree select && select.getExpression() == tree;
                        String name = tree.toString();
                        if (!partOfLargerName && isOrca(name)
                                && ALLOWED.stream().noneMatch(owner -> name.equals(owner) || name.startsWith(owner + "."))) reject();
                        return super.visitMemberSelect(tree, unused);
                    }
                }.scan(unit, null);
            }
            if (diagnostics.getDiagnostics().stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR)) {
                throw new IllegalArgumentException("Consumer Java syntax could not be verified");
            }
        }
    }
    private static boolean isOrca(String name) { return name.equals(ROOT) || name.startsWith(ROOT + "."); }
    private static void reject() { throw new IllegalArgumentException("Unsupported Orca source dependency"); }
}
