#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WRAPPER_JAR="$REPO_ROOT/gradle/wrapper/gradle-wrapper.jar"
WRAPPER_URL="https://repo.gradle.org/artifactory/libs-releases-local/org/gradle/gradle-wrapper/6.1.1/gradle-wrapper-6.1.1-sources.jar"
mkdir -p "$(dirname "$WRAPPER_JAR")"

if command -v curl >/dev/null 2>&1; then
  curl -sSfL "$WRAPPER_URL" -o "$WRAPPER_JAR"
elif command -v wget >/dev/null 2>&1; then
  wget -q "$WRAPPER_URL" -O "$WRAPPER_JAR"
else
  echo "Neither curl nor wget is available. Please download $WRAPPER_URL manually to $WRAPPER_JAR" >&2
  exit 1
fi

echo "Gradle wrapper JAR bootstrapped at $WRAPPER_JAR"
