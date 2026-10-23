#!/bin/sh
set -eu
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if [ -f "$PROJECT_DIR/gradle/wrapper/gradle-wrapper.jar" ]; then
    exec java ${JAVA_OPTS:-} -classpath "$PROJECT_DIR/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
fi
if command -v gradle >/dev/null 2>&1; then
    exec gradle -p "$PROJECT_DIR" "$@"
fi
echo 'Gradle fehlt. Gradle 8.13 installieren oder tools/bootstrap.sh ausführen.' >&2
exit 1
