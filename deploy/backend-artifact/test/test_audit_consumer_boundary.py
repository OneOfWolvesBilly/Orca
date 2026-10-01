"""Black-box RED tests for the release verifier's consumer dependency boundary.

Maven is a controlled successful compiler double. These tests prove verifier
orchestration only, never artifact resolution, Java compilation, or publication.
"""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
from artifact_fixture import make_fixture

ROOT = Path(__file__).resolve().parents[1]
AUDIT = "io.github.oneofwolvesbilly.orca.referencecore.application."


class AuditConsumerBoundaryTest(unittest.TestCase):
    def setUp(self):
        self.workspace = tempfile.TemporaryDirectory(prefix="orca-audit-boundary-test-")
        self.addCleanup(self.workspace.cleanup)
        self.root = Path(self.workspace.name)
        self.release = self.root / "release"
        shutil.copytree(ROOT / "bin", self.release / "bin")
        self.fixture = self.release / "consumer-fixture"
        self.source = self.fixture / "src/test/java/example/Consumer.java"
        self.source.parent.mkdir(parents=True)
        (self.fixture / "pom.xml").write_text("<project/>\n")
        self.repository = self.root / "repository"
        self.repository.mkdir()
        self.manifest = self.root / "expected.json"
        make_fixture(self.repository, self.manifest)
        self.called = self.root / "maven-called"
        self.maven = self.root / "maven-double"
        self.maven.write_text('''#!/usr/bin/env python3
import os, pathlib, sys, shutil
import subprocess
args = sys.argv[1:]
pathlib.Path(os.environ["AUDIT_TEST_CALLED"]).write_text("called")
pom = pathlib.Path(args[args.index("-f") + 1])
cache = pathlib.Path(next(a.split("=", 1)[1] for a in args if a.startswith("-Dmaven.repo.local=")))
assert cache.is_dir() and not list(cache.iterdir()), "Dependency cache must be fresh"
assert not (pom.parent / "target").exists(), "Stale target must not enter copied fixture"
shutil.copytree(os.environ["AUDIT_TEST_REPOSITORY"], cache, dirs_exist_ok=True)
subprocess.run(["python3", os.environ["AUDIT_TEST_HELPER"], str(pom.parent)], check=True)
''')
        self.maven.chmod(0o755)

    def verify(self, source):
        self.source.write_text(source)
        return subprocess.run(
            ["sh", str(self.release / "bin/verify-release-candidate.sh"), "1.2.3", self.repository.as_uri()],
            env={**os.environ, "ORCA_RELEASE_MAVEN_COMMAND": str(self.maven),
                 "ORCA_RELEASE_MAVEN_SETTINGS": "", "AUDIT_TEST_CALLED": str(self.called),
                 "AUDIT_TEST_HELPER": str(ROOT / "test/artifact_fixture.py"), "AUDIT_TEST_REPOSITORY": str(self.repository), "ORCA_RELEASE_EXPECTED_MANIFEST": str(self.manifest)},
            capture_output=True, text=True, timeout=30)

    def assert_allowed(self, source):
        result = self.verify(source)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertTrue(self.called.exists(), "Positive control must reach Maven")

    def assert_forbidden(self, source):
        result = self.verify(source)
        self.assertNotEqual(0, result.returncode, "Unsupported dependency was accepted")
        self.assertFalse(self.called.exists(), "Source boundary must fail before successful Maven proof")

    def test_allowed_exact_audit_type(self):
        self.assert_allowed(f"import {AUDIT}AuditRecord; class Consumer {{ AuditRecord value; }}")

    def test_allowed_static_member(self):
        self.assert_allowed(f"import static {AUDIT}AuditEventType.of; class Consumer {{ Object value = of(\"event\"); }}")

    def test_allowed_fully_qualified_type(self):
        self.assert_allowed(f"class Consumer {{ {AUDIT}AuditRecord value; }}")

    def test_comments_and_literals_are_not_dependencies(self):
        self.assert_allowed('class Consumer { String s = "io.github.oneofwolvesbilly.orca.internal.Hidden"; }'
                            '\n// import io.github.oneofwolvesbilly.orca.internal.Hidden;\n')

    def test_forbidden_ordinary_import(self):
        self.assert_forbidden("import io.github.oneofwolvesbilly.orca.auth.domain.Actor; class Consumer {}")

    def test_forbidden_static_import(self):
        self.assert_forbidden("import static io.github.oneofwolvesbilly.orca.internal.Hidden.value; class Consumer {}")

    def test_forbidden_fully_qualified_reference(self):
        self.assert_forbidden("class Consumer { io.github.oneofwolvesbilly.orca.internal.Hidden value; }")

    def test_forbidden_same_package_sibling(self):
        self.assert_forbidden(f"import {AUDIT}UnapprovedSibling; class Consumer {{}}")

    def test_forbidden_package_wildcard(self):
        self.assert_forbidden(f"import {AUDIT}*; class Consumer {{}}")

    def test_forbidden_static_wildcard(self):
        self.assert_forbidden(f"import static {AUDIT}AuditRecord.*; class Consumer {{}}")

    def test_main_sources_are_guarded_too(self):
        self.source = self.fixture / "src/main/java/example/Consumer.java"
        self.source.parent.mkdir(parents=True)
        self.assert_forbidden("import io.github.oneofwolvesbilly.orca.internal.Hidden; class Consumer {}")

    def test_missing_expected_evidence_fails_before_maven(self):
        self.manifest.unlink()
        result = self.verify("class Consumer {}")
        self.assertNotEqual(0, result.returncode)
        self.assertFalse(self.called.exists())

    def test_artifact_substitution_cannot_pass_on_maven_exit_code(self):
        jar = self.repository / "io/github/oneofwolvesbilly/orca/1.2.3/orca-1.2.3.jar"
        jar.write_bytes(jar.read_bytes() + b"substitution")
        result = self.verify("class Consumer {}")
        self.assertTrue(self.called.exists())
        self.assertNotEqual(0, result.returncode)

    def test_java_unicode_escape_cannot_hide_forbidden_import(self):
        self.assert_forbidden(r"import io.github.oneofwolvesbilly.orc\u0061.internal.Hidden; class Consumer {}")

    def test_malformed_source_is_not_a_successful_guard(self):
        self.assert_forbidden("class Consumer { this is not Java }")

    def test_declaring_orca_package_is_not_consumer_code(self):
        self.assert_forbidden("package io.github.oneofwolvesbilly.orca.internal; class Consumer {}")

    def test_stale_fixture_output_is_excluded(self):
        stale = self.fixture / "target/classes/Stale.class"
        stale.parent.mkdir(parents=True)
        stale.write_bytes(b"synthetic stale bytecode")
        self.assert_allowed("class Consumer {}")


class AuditCompatibilityMetadataTest(unittest.TestCase):
    def test_current_release_template_declares_exact_public_api(self):
        template = ROOT.parents[1] / "orca_backend/src/main/release-filtered-resources/META-INF/orca/backend-artifact-compatibility.properties"
        properties = dict(line.split("=", 1) for line in template.read_text().splitlines() if "=" in line)
        declaration = properties.get("audit.public-api")
        self.assertIsNotNone(declaration, "Current source release template has no audit API declaration")
        names = [name.strip() for name in declaration.split(",")]
        self.assertEqual(7, len(names))
        self.assertEqual({AUDIT + name for name in ("AuditActorId", "AuditEventType", "AuditOutcome",
                         "AuditMetadataEntry", "AuditMetadata", "AuditRecord", "AuditRecorder")}, set(names))


if __name__ == "__main__":
    unittest.main()
