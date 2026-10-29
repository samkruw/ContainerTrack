#!/usr/bin/env bash
# Invoke once: android-emulator-runner executes separate script lines in separate shells.
set -eu
chmod +x gradlew
adb shell wm size 1080x2340
test_status=0
./gradlew connectedDebugAndroidTest --no-daemon --stacktrace -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true || test_status=$?
mkdir -p app/build/screenshots
adb pull /sdcard/Android/data/ch.zerogen.containertrack/files/screenshots/. app/build/screenshots/ || true
exit "$test_status"
