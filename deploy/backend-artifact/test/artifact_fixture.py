"""Synthetic inventory data; no compilation, runtime, or publication claim."""
import hashlib
import json
from pathlib import Path
import zipfile

GROUP = "io.github.oneofwolvesbilly"
PREFIX = GROUP + ".orca.referencecore.application."
API = [PREFIX + name for name in ("AuditActorId", "AuditEventType", "AuditOutcome", "AuditMetadataEntry", "AuditMetadata", "AuditRecord", "AuditRecorder")]
VERSION = "1.2.3"
COMMIT = "a" * 40


def make_fixture(repository, manifest):
    directory = Path(repository) / "io/github/oneofwolvesbilly/orca" / VERSION
    directory.mkdir(parents=True, exist_ok=True)
    metadata = {"orca.version": VERSION, "orca.source-commit": COMMIT,
                "java.release": "21", "java.verified-runtimes": "22.0.2",
                "spring-boot.supported": "4.0.1", "database.product": "MariaDB",
                "database.verified-versions": "11.4.5", "migration.current": "V8",
                "migration.supported-starts": "empty,current",
                "embedded-auth.public-api": "AuthenticatedActor,EnableOrcaEmbeddedAuth,OrcaProtectedCommand",
                "audit.public-api": ",".join(API), "unsupported-combinations": "Java below 21"}
    for suffix, extension in [(".jar", ".class"), ("-sources.jar", ".java"), ("-javadoc.jar", ".html")]:
        with zipfile.ZipFile(directory / ("orca-" + VERSION + suffix), "w") as jar:
            for name in API:
                jar.writestr(name.replace(".", "/") + extension, "synthetic inventory member")
            if suffix == ".jar":
                jar.writestr("META-INF/orca/backend-artifact-compatibility.properties",
                             "\n".join(k + "=" + v for k, v in metadata.items()))
    (directory / ("orca-" + VERSION + ".pom")).write_text(
        '<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion>'
        f'<groupId>{GROUP}</groupId><artifactId>orca</artifactId><version>{VERSION}</version></project>')
    (directory / "_remote.repositories").write_text("\n".join(
        p.name + ">orca-under-test=" for p in directory.iterdir() if p.suffix in (".jar", ".pom")))
    evidence = {"format": 1, "coordinate": GROUP + ":orca:" + VERSION, "sourceCommit": COMMIT,
                "evidenceLevel": "repository-local", "compatibility": metadata,
                "sha256": {p.name: hashlib.sha256(p.read_bytes()).hexdigest()
                           for p in directory.iterdir() if p.suffix in (".jar", ".pom")}}
    Path(manifest).write_text(json.dumps(evidence))
    return directory


def make_reports(consumer):
    reports = Path(consumer) / "target/surefire-reports"
    reports.mkdir(parents=True, exist_ok=True)
    for name, count in [("AuditArtifactConsumerTest", 6), ("AuditPublicCompilationTest", 3), ("PublishedArtifactConsumerTest", 4)]:
        full_name = "io.github.oneofwolvesbilly.orcaconsumer." + name
        (reports / ("TEST-" + full_name + ".xml")).write_text(
            f'<testsuite name="{full_name}" tests="{count}" failures="0" errors="0" skipped="0"/>')


if __name__ == "__main__":
    import sys
    if len(sys.argv) == 2:
        make_reports(sys.argv[1])
    else:
        make_fixture(sys.argv[1], sys.argv[2])
