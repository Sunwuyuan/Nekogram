import contextlib
import io
from pathlib import Path
import tempfile
import unittest
import zipfile

from verify_debug_apk import verify_apk


class VerifyDebugApkTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory(prefix="apk-verifier-test-")
        self.addCleanup(self.directory.cleanup)
        self.apk = Path(self.directory.name) / "fixture.apk"

    def write_fixture(self, entries):
        with zipfile.ZipFile(self.apk, "w") as apk:
            for entry, content in entries.items():
                apk.writestr(entry, content)

    def test_requires_an_apk_file(self):
        with self.assertRaises(ValueError):
            verify_apk(self.apk)

    def test_rejects_non_zip_files(self):
        self.apk.write_bytes(b"not an APK")
        with self.assertRaises(zipfile.BadZipFile):
            verify_apk(self.apk)

    def test_requires_manifest_dex_and_native_code(self):
        entries = {
            "AndroidManifest.xml": b"fixture manifest",
            "classes.dex": b"fixture dex",
            "lib/arm64-v8a/libtmessages.49.so": b"fixture native code",
        }
        for missing in entries:
            with self.subTest(missing=missing):
                self.write_fixture({key: value for key, value in entries.items() if key != missing})
                with self.assertRaises(ValueError):
                    verify_apk(self.apk)

    def test_rejects_empty_entries(self):
        self.write_fixture({
            "AndroidManifest.xml": b"fixture manifest",
            "classes.dex": b"",
            "lib/arm64-v8a/libtmessages.49.so": b"fixture native code",
        })
        with self.assertRaises(ValueError):
            verify_apk(self.apk)

    def test_accepts_complete_archive_structure(self):
        self.write_fixture({
            "AndroidManifest.xml": b"fixture manifest",
            "classes.dex": b"fixture dex",
            "lib/arm64-v8a/libtmessages.49.so": b"fixture native code",
        })
        with contextlib.redirect_stdout(io.StringIO()):
            verify_apk(self.apk)


if __name__ == "__main__":
    unittest.main()
