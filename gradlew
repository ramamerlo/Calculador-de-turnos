#!/usr/bin/env sh
set -e
APP_HOME=$(cd "$(dirname "$0")" && pwd)
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$WRAPPER_JAR" ]; then
    if command -v gradle >/dev/null 2>&1; then
        gradle wrapper --gradle-version 8.7 --project-dir "$APP_HOME"
    fi
fi

if [ -f "$WRAPPER_JAR" ]; then
    exec java -Xmx64m -Xms64m -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
else
    exec gradle "$@"
fi
