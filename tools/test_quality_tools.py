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

    def privacy(self, extra_manifest='', extra_code='', merged=False, code_path='Example.kt'):
        if (self.fixture / 'app').exists():
            shutil.rmtree(self.fixture / 'app')
        main = self.fixture / 'app/src/main'
        main.mkdir(parents=True)
        source = (ROOT / 'app/src/main/AndroidManifest.xml').read_text()
        source = source.replace('<application ', extra_manifest + '<application ', 1)
        (main / 'AndroidManifest.xml').write_text(source)
        kotlin = main / 'java'
        kotlin.mkdir()
        target = kotlin / code_path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text('package test\n' + extra_code)
        (self.fixture / 'gradle').mkdir(exist_ok=True)
        shutil.copy(ROOT / 'gradle/libs.versions.toml', self.fixture / 'gradle/libs.versions.toml')
        if merged:
            target = self.fixture / 'app/build/intermediates/merged_manifests/debug/processDebugManifest'
            target.mkdir(parents=True)
            shutil.copy(main / 'AndroidManifest.xml', target / 'AndroidManifest.xml')
            tests = self.fixture / 'app/build/intermediates/merged_manifests/debugAndroidTest/processDebugAndroidTestManifest'
            tests.mkdir(parents=True)
            (tests / 'AndroidManifest.xml').write_text('<manifest><application /></manifest>')
        spec = importlib.util.spec_from_file_location('privacy', ROOT / 'tools/check_privacy.py')
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        module.ROOT = self.fixture
        original = sys.argv
        try:
            sys.argv = ['check_privacy.py'] + (['--merged'] if merged else [])
            with contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()):
                return module.main()
        finally:
            sys.argv = original

    def test_removal_markers_do_not_grant_network_permission(self):
        self.assertEqual(0, self.privacy())

    def test_unexpected_location_permission_is_rejected(self):
        self.assertEqual(1, self.privacy('<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />'))

    def test_network_is_only_allowed_in_the_graph_adapter(self):
        self.assertEqual(0, self.privacy(extra_code='val endpoint = "https://graph.microsoft.com/v1.0"',
            code_path='ch/zerogen/containertrack/GraphPhotoClient.kt'))

    def test_private_keys_and_webview_stay_forbidden(self):
        self.assertEqual(1, self.privacy(extra_code='val key = "-----BEGIN PRIVATE KEY-----"'))
        self.assertEqual(1, self.privacy(extra_code='val view = WebView(context)',
            code_path='ch/zerogen/containertrack/GraphPhotoClient.kt'))

    def test_production_network_call_is_rejected(self):
        self.assertEqual(1, self.privacy(extra_code='val url = "https://example.invalid"'))

    def test_merged_gate_ignores_android_test_manifest(self):
        self.assertEqual(0, self.privacy(merged=True))

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
