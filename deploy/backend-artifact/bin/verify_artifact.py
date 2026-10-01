"""Validate retrieved components against separately retained release evidence.

The expected manifest is a trusted build/review input, never regenerated from
retrieved bytes during verification. Checksums establish identity, not authorship.
"""
import hashlib
import json
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET
import zipfile

GROUP = "io.github.oneofwolvesbilly"
AUDIT_PREFIX = GROUP + ".orca.referencecore.application."
AUDIT_API = {AUDIT_PREFIX + name for name in ("AuditActorId", "AuditEventType", "AuditOutcome",
             "AuditMetadataEntry", "AuditMetadata", "AuditRecord", "AuditRecorder")}
METADATA = "META-INF/orca/backend-artifact-compatibility.properties"


def require(condition, message):
    if not condition:
        raise ValueError(message)


def unique_pairs(pairs):
    result = {}
    for key, value in pairs:
        require(key not in result, "Duplicate evidence key")
        result[key] = value
    return result


def filenames(version):
    return ["orca-" + version + suffix for suffix in (".jar", ".pom", "-sources.jar", "-javadoc.jar")]


def read_manifest(path, version):
    evidence = json.loads(Path(path).read_text(), object_pairs_hook=unique_pairs)
    require(evidence.get("format") == 1, "Unsupported evidence format")
    require(evidence.get("coordinate") == GROUP + ":orca:" + version, "Expected coordinate mismatch")
    require(re.fullmatch(r"[0-9a-f]{40}", str(evidence.get("sourceCommit", ""))), "Invalid source commit")
    require(evidence.get("evidenceLevel") in ("repository-local", "staged"), "Invalid build evidence level")
    hashes = evidence.get("sha256", {})
    require(set(hashes) == set(filenames(version)), "Incomplete expected component set")
    require(all(isinstance(value, str) and re.fullmatch(r"[0-9a-f]{64}", value) for value in hashes.values()), "Invalid component digest")
    require(isinstance(evidence.get("compatibility"), dict), "Missing compatibility evidence")
    return evidence


def inspect(repository, version):
    require(re.fullmatch(r"(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)", version), "Invalid exact version")
    directory = Path(repository) / "io/github/oneofwolvesbilly/orca" / version
    require(not any(p.is_dir() and p.name != version for p in directory.parent.iterdir()), "Multiple Orca versions in isolated repository")
    metadata = None
    hashes = {}
    for name in filenames(version):
        path = directory / name
        require(path.is_file() and not path.is_symlink(), "Missing or substituted component")
        hashes[name] = hashlib.sha256(path.read_bytes()).hexdigest()
        if name.endswith(".pom"):
            root = ET.fromstring(path.read_bytes())
            ns = "{http://maven.apache.org/POM/4.0.0}"
            require([root.findtext(ns + key) for key in ("groupId", "artifactId", "version")]
                    == [GROUP, "orca", version], "Consumer POM coordinate mismatch")
            require(b"${revision}" not in path.read_bytes() and b"${changelist}" not in path.read_bytes(), "Unresolved POM version")
            continue
        with zipfile.ZipFile(path) as jar:
            names = jar.namelist()
            require(len(names) == len(set(names)), "Duplicate archive member")
            require(jar.testzip() is None, "Corrupt archive member")
            extension = ".java" if name.endswith("-sources.jar") else ".html" if name.endswith("-javadoc.jar") else ".class"
            require(all(api.replace(".", "/") + extension in names for api in AUDIT_API), "Missing audit API member")
            require(not any("RecordingAuditRecorder" in item or "orcaconsumer/" in item or "orcafixture/" in item
                            or item.endswith("Test.class") or item.endswith("Test.java") for item in names), "Test asset in artifact")
            if extension == ".class":
                lines = jar.read(METADATA).decode("utf-8").splitlines()
                metadata = unique_pairs(tuple(line.split("=", 1)) for line in lines if line and not line.startswith("#"))
    require(metadata is not None, "Missing compatibility metadata")
    require(metadata.get("orca.version") == version, "Artifact version mismatch")
    require(re.fullmatch(r"[0-9a-f]{40}", metadata.get("orca.source-commit", "")), "Invalid artifact source commit")
    declared = [name.strip() for name in metadata.get("audit.public-api", "").split(",")]
    require(len(declared) == 7 and set(declared) == AUDIT_API, "Audit API declaration mismatch")
    for key in ("java.release", "java.verified-runtimes", "spring-boot.supported", "database.product",
                "database.verified-versions", "migration.current", "migration.supported-starts", "embedded-auth.public-api"):
        require(metadata.get(key) and metadata[key] != "UNSET" and "@" not in metadata[key], "Incomplete compatibility metadata")
    return directory, metadata, hashes


def verify(repository, version, manifest):
    expected = read_manifest(manifest, version)
    directory, metadata, hashes = inspect(repository, version)
    require(hashes == expected["sha256"], "Component checksum mismatch")
    require(metadata["orca.source-commit"] == expected["sourceCommit"], "Source commit mismatch")
    require(metadata == expected["compatibility"], "Compatibility evidence mismatch")
    origins = (directory / "_remote.repositories").read_text().splitlines()
    require(all(name + ">orca-under-test=" in origins for name in filenames(version)), "Component resolved outside selected repository")
    return expected


def create(repository, version, source_commit, level, output):
    _, metadata, hashes = inspect(repository, version)
    require(metadata["orca.source-commit"] == source_commit, "Build source commit mismatch")
    require(level in ("repository-local", "staged"), "Invalid evidence level")
    evidence = dict(format=1, coordinate=GROUP + ":orca:" + version, sourceCommit=source_commit,
                    evidenceLevel=level, compatibility=metadata, sha256=hashes)
    # Never replace already reviewed evidence for a candidate.
    with Path(output).open("x") as stream:
        json.dump(evidence, stream, indent=2, sort_keys=True)
        stream.write("\n")


def results(consumer, manifest, output):
    expected = json.loads(Path(manifest).read_text(), object_pairs_hook=unique_pairs)
    suites = {"AuditArtifactConsumerTest": 6, "AuditPublicCompilationTest": 3, "PublishedArtifactConsumerTest": 4}
    counts = {}
    for name, minimum in suites.items():
        report = Path(consumer) / "target/surefire-reports" / ("TEST-io.github.oneofwolvesbilly.orcaconsumer." + name + ".xml")
        root = ET.fromstring(report.read_bytes())
        require(root.get("name") == "io.github.oneofwolvesbilly.orcaconsumer." + name, "Unexpected test report")
        count = int(root.get("tests", "0"))
        require(count >= minimum and all(root.get(key) == "0" for key in ("failures", "errors", "skipped")), "Incomplete consumer test proof")
        counts[name] = count
    if output:
        with Path(output).open("x") as stream:
            json.dump({"coordinate": expected["coordinate"], "sourceCommit": expected["sourceCommit"],
                       "buildEvidenceLevel": expected["evidenceLevel"],
                       "sourceCommitRole": "base-only" if expected["evidenceLevel"] == "repository-local" else "candidate",
                       "sha256": expected["sha256"],
                       "consumerTests": counts, "releaseComplete": False}, stream, indent=2, sort_keys=True)
            stream.write("\n")
    print("Consumer suites passed: " + ", ".join(name + "=" + str(count) for name, count in counts.items()))


def main():
    command, *args = sys.argv[1:]
    if command == "verify" and len(args) == 3:
        verify(*args)
    elif command == "check-manifest" and len(args) == 2:
        read_manifest(*args)
    elif command == "check-results" and len(args) == 3:
        results(*args)
    elif command == "check-release-manifest" and len(args) == 2:
        require(read_manifest(*args)["evidenceLevel"] == "staged", "Publication proof requires retained staged evidence")
    elif command == "create" and len(args) == 5:
        create(*args)
    else:
        raise ValueError("Invalid artifact verification command")


if __name__ == "__main__":
    try:
        main()
    except Exception:
        # Artifact bytes, metadata and parser errors are untrusted and may contain secrets.
        print("Artifact inventory or provenance verification failed.", file=sys.stderr)
        sys.exit(1)
