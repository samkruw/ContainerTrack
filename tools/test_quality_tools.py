"""Tests for release-gate tooling; these do not run the Android app."""
import contextlib
import importlib.util
import io
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]

class QualityToolsTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.fixture = Path(self.temp.name)

    def privacy(self, extra_manifest='', extra_code=''):
        main = self.fixture / 'app/src/main'
        main.mkdir(parents=True)
        source = (ROOT / 'app/src/main/AndroidManifest.xml').read_text()
        source = source.replace('<application ', extra_manifest + '<application ', 1)
        (main / 'AndroidManifest.xml').write_text(source)
        kotlin = main / 'java'
        kotlin.mkdir()
        (kotlin / 'Example.kt').write_text('package test\n' + extra_code)
        (self.fixture / 'gradle').mkdir()
        shutil.copy(ROOT / 'gradle/libs.versions.toml', self.fixture / 'gradle/libs.versions.toml')
        spec = importlib.util.spec_from_file_location('privacy', ROOT / 'tools/check_privacy.py')
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        module.ROOT = self.fixture
        original = sys.argv
        try:
            sys.argv = ['check_privacy.py']
            with contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()):
                return module.main()
        finally:
            sys.argv = original

    def test_removal_markers_do_not_grant_network_permission(self):
        self.assertEqual(0, self.privacy())

    def test_actual_network_permission_is_rejected(self):
        self.assertEqual(1, self.privacy('<uses-permission android:name="android.permission.INTERNET" />'))

    def test_production_network_call_is_rejected(self):
        self.assertEqual(1, self.privacy(extra_code='val url = "https://example.invalid"'))

    def test_screenshot_gate_checks_match_difference_and_missing(self):
        goldens, actual = self.fixture / 'goldens', self.fixture / 'actual'
        goldens.mkdir(); actual.mkdir()
        names = ('01-start','02-delivery-setup','03-ocr-scan','04-delivery-photo',
                 '05-return-logger','06-return-setup','07-export','08-logger-due')
        for name in names:
            for folder in (goldens, actual):
                Image.new('RGB', (8, 8), 'white').save(folder / f'{name}.png')
        command = [sys.executable, str(ROOT / 'tools/compare_screenshots.py'), str(goldens), str(actual)]
        self.assertEqual(0, subprocess.run(command, capture_output=True).returncode)
        Image.new('RGB', (8, 8), 'black').save(actual / '01-start.png')
        self.assertEqual(1, subprocess.run(command, capture_output=True).returncode)
        self.assertTrue((actual / '01-start-diff.png').exists())
        (actual / '02-delivery-setup.png').unlink()
        self.assertEqual(1, subprocess.run(command, capture_output=True).returncode)

if __name__ == '__main__':
    unittest.main()
