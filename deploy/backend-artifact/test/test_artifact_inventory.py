import hashlib
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
import zipfile
from artifact_fixture import make_fixture, API, VERSION, COMMIT

SCRIPT = Path(__file__).resolve().parents[1] / "bin/verify_artifact.py"


class ArtifactInventoryTest(unittest.TestCase):
    def setUp(self):
        self.workspace = tempfile.TemporaryDirectory(prefix="orca-artifact-inventory-")
        self.addCleanup(self.workspace.cleanup)
        self.root = Path(self.workspace.name)
        self.manifest = self.root / "expected.json"
        self.directory = make_fixture(self.root / "repository", self.manifest)

    def verify(self):
        self.assertTrue(SCRIPT.is_file(), "Artifact verifier implementation is missing")
        return subprocess.run(["python3", str(SCRIPT), "verify", str(self.root / "repository"),
                               VERSION, str(self.manifest)], capture_output=True, text=True, timeout=20)

    def refresh_hash(self, path):
        evidence = json.loads(self.manifest.read_text())
        evidence["sha256"][path.name] = hashlib.sha256(path.read_bytes()).hexdigest()
        self.manifest.write_text(json.dumps(evidence))

    def rewrite_jar(self, suffix, change):
        path = self.directory / ("orca-" + VERSION + suffix)
        with zipfile.ZipFile(path) as jar:
            entries = {name: jar.read(name) for name in jar.namelist()}
        change(entries)
        with zipfile.ZipFile(path, "w") as jar:
            for name, data in entries.items():
                jar.writestr(name, data)
        self.refresh_hash(path)

    def assert_rejected(self):
        result = self.verify()
        self.assertNotEqual(0, result.returncode)
        self.assertNotIn("synthetic-private-value", result.stdout + result.stderr)

    def test_complete_inventory(self):
        result = self.verify()
        self.assertEqual(0, result.returncode, result.stderr)

    def test_each_missing_component(self):
        for suffix in (".jar", ".pom", "-sources.jar", "-javadoc.jar"):
            with self.subTest(suffix=suffix):
                path = self.directory / ("orca-" + VERSION + suffix)
                content = path.read_bytes()
                path.unlink()
                self.assert_rejected()
                path.write_bytes(content)

    def test_corrupt_archive_with_matching_checksum(self):
        path = self.directory / ("orca-" + VERSION + ".jar")
        path.write_bytes(b"not a zip")
        self.refresh_hash(path)
        self.assert_rejected()

    def test_substituted_bytes_fail_pinned_checksum(self):
        path = self.directory / ("orca-" + VERSION + "-sources.jar")
        path.write_bytes(path.read_bytes() + b"substitution")
        self.assert_rejected()

    def test_omitted_audit_class(self):
        self.rewrite_jar(".jar", lambda entries: entries.pop(API[0].replace(".", "/") + ".class"))
        self.assert_rejected()

    def test_omitted_source(self):
        self.rewrite_jar("-sources.jar", lambda entries: entries.pop(API[0].replace(".", "/") + ".java"))
        self.assert_rejected()

    def test_omitted_documentation(self):
        self.rewrite_jar("-javadoc.jar", lambda entries: entries.pop(API[0].replace(".", "/") + ".html"))
        self.assert_rejected()

    def alter_metadata(self, key, value):
        name = "META-INF/orca/backend-artifact-compatibility.properties"
        def change(entries):
            lines = entries[name].decode().splitlines()
            entries[name] = "\n".join(key + "=" + value if line.startswith(key + "=") else line for line in lines).encode()
        self.rewrite_jar(".jar", change)

    def test_inconsistent_api_declaration(self):
        self.alter_metadata("audit.public-api", ",".join(API[:-1]))
        self.assert_rejected()

    def test_stale_source_commit(self):
        self.alter_metadata("orca.source-commit", "b" * 40)
        self.assert_rejected()

    def test_mismatched_jar_version(self):
        self.alter_metadata("orca.version", "1.2.4")
        self.assert_rejected()

    def test_mismatched_pom_coordinate(self):
        path = self.directory / ("orca-" + VERSION + ".pom")
        path.write_text(path.read_text().replace("<artifactId>orca</artifactId>", "<artifactId>other</artifactId>"))
        self.refresh_hash(path)
        self.assert_rejected()

    def test_malformed_pom(self):
        path = self.directory / ("orca-" + VERSION + ".pom")
        path.write_text("<broken>synthetic-private-value")
        self.refresh_hash(path)
        self.assert_rejected()

    def test_test_recorder_is_not_a_production_component(self):
        self.rewrite_jar(".jar", lambda entries: entries.update({"example/RecordingAuditRecorder.class": b"test"}))
        self.assert_rejected()

    def test_duplicate_archive_member(self):
        path = self.directory / ("orca-" + VERSION + ".jar")
        import warnings
        with warnings.catch_warnings():
            warnings.simplefilter("ignore", UserWarning)
            with zipfile.ZipFile(path, "a") as jar:
                jar.writestr(API[0].replace(".", "/") + ".class", "duplicate")
        self.refresh_hash(path)
        self.assert_rejected()

    def test_wrong_resolution_origin(self):
        (self.directory / "_remote.repositories").write_text("orca-1.2.3.jar>central=\n")
        self.assert_rejected()

    def test_local_install_cannot_replace_repository_resolution(self):
        (self.directory / "_remote.repositories").unlink()
        self.assert_rejected()

    def test_conflicting_versions_in_isolated_repository(self):
        (self.directory.parent / "1.2.4").mkdir()
        self.assert_rejected()

    def test_runtime_evidence_mismatch(self):
        self.alter_metadata("java.verified-runtimes", "21.0.9")
        self.assert_rejected()

    def test_malformed_expected_evidence(self):
        self.manifest.write_text('{"format":1,"sourceCommit":"synthetic-private-value"}')
        self.assert_rejected()

    def test_duplicate_expected_evidence_key(self):
        self.manifest.write_text(self.manifest.read_text().replace('"format": 1', '"format": 1, "format": 1'))
        self.assert_rejected()


class ConsumerEvidenceTest(unittest.TestCase):
    def setUp(self):
        from artifact_fixture import make_reports
        self.workspace = tempfile.TemporaryDirectory(prefix="orca-consumer-evidence-")
        self.addCleanup(self.workspace.cleanup)
        self.root = Path(self.workspace.name)
        self.manifest = self.root / "expected.json"
        make_fixture(self.root / "repository", self.manifest)
        self.consumer = self.root / "consumer"
        make_reports(self.consumer)
        self.output = self.root / "result.json"
        self.report = self.consumer / "target/surefire-reports/TEST-io.github.oneofwolvesbilly.orcaconsumer.AuditArtifactConsumerTest.xml"

    def result(self):
        return subprocess.run(["python3", str(SCRIPT), "check-results", str(self.consumer),
                               str(self.manifest), str(self.output)], capture_output=True, text=True)

    def test_complete_reports_produce_bounded_evidence(self):
        result = self.result()
        self.assertEqual(0, result.returncode, result.stderr)
        evidence = json.loads(self.output.read_text())
        self.assertEqual(6, evidence["consumerTests"]["AuditArtifactConsumerTest"])
        self.assertFalse(evidence["releaseComplete"])

    def test_missing_report_cannot_be_maven_success(self):
        self.report.unlink()
        self.assertNotEqual(0, self.result().returncode)
        self.assertFalse(self.output.exists())

    def test_skipped_audit_test_cannot_be_success(self):
        self.report.write_text(self.report.read_text().replace('skipped="0"', 'skipped="1"'))
        self.assertNotEqual(0, self.result().returncode)

    def test_failing_audit_test_cannot_be_success(self):
        self.report.write_text(self.report.read_text().replace('failures="0"', 'failures="1"'))
        self.assertNotEqual(0, self.result().returncode)

    def test_incomplete_suite_cannot_be_success(self):
        self.report.write_text(self.report.read_text().replace('tests="6"', 'tests="0"'))
        self.assertNotEqual(0, self.result().returncode)

    def test_evidence_is_not_overwritten(self):
        self.output.write_text("retained evidence")
        self.assertNotEqual(0, self.result().returncode)
        self.assertEqual("retained evidence", self.output.read_text())

    def test_development_evidence_cannot_be_promoted_to_published_proof(self):
        result = subprocess.run(["python3", str(SCRIPT), "check-release-manifest", str(self.manifest), VERSION], capture_output=True)
        self.assertNotEqual(0, result.returncode)
