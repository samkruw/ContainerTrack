"""The emulator action starts each script line in a separate shell."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


SCRIPT = Path(__file__).with_name('run_device_tests.sh').resolve()


class DeviceRunnerTest(unittest.TestCase):
    def run_fixture(self, result):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / 'gradlew').write_text('#!/bin/sh\nexit "$FIXTURE_RESULT"\n')
            adb = root / 'adb'
            adb.write_text('#!/bin/sh\nif [ "$1" = pull ]; then touch pulled; fi\nexit 0\n')
            adb.chmod(0o755)
            env = dict(os.environ, PATH=f'{root}{os.pathsep}{os.environ["PATH"]}', FIXTURE_RESULT=str(result))
            run = subprocess.run(['bash', str(SCRIPT)], cwd=root, env=env, capture_output=True, text=True)
            self.assertEqual(result, run.returncode, run.stderr)
            self.assertTrue((root / 'pulled').exists(), 'Capture collection must run even after a failed test')

    def test_success_returns_zero_and_collects_captures(self):
        self.run_fixture(0)

    def test_failure_preserves_status_and_collects_captures(self):
        self.run_fixture(9)
