#!/usr/bin/env sh

set -e

APP_HOME="$(cd "$(dirname "$0")" && pwd)"
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
WRAPPER_URL="https://repo.gradle.org/gradle/libs-releases-local/org/gradle/gradle-wrapper/8.7/gradle-wrapper-8.7.jar"

if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Gradle wrapper JAR missing. Attempting download..."
    mkdir -p "$(dirname "$WRAPPER_JAR")"
    if command -v curl >/dev/null 2>&1 ; then
        curl -sSfL "$WRAPPER_URL" -o "$WRAPPER_JAR"
    elif command -v wget >/dev/null 2>&1 ; then
        wget -q "$WRAPPER_URL" -O "$WRAPPER_JAR"
    else
        echo "ERROR: gradle-wrapper.jar missing and neither curl nor wget are available." >&2
        exit 1
    fi
fi

if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

if ! command -v "$JAVACMD" >/dev/null 2>&1 ; then
    echo "ERROR: Java executable '$JAVACMD' could not be found. Set JAVA_HOME or ensure java is on PATH." >&2
    exit 1
fi

CLASSPATH="$WRAPPER_JAR"

exec "$JAVACMD" ${JAVA_OPTS} ${GRADLE_OPTS} -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
