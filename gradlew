#!/usr/bin/env sh
# Minimal gradlew stub that bootstraps via system gradle if wrapper jar missing
set -e
DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$DIR/gradle/wrapper/gradle-wrapper.jar"
if [ -f "$JAR" ]; then
  exec java -jar "$JAR" "$@"
else
  echo "Wrapper jar not found, trying system gradle..."
  if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
  else
    echo "No gradle found. Generating wrapper via SDKMAN or installing gradle..."
    echo "On GitHub Actions this is auto-handled. Locally: open in Android Studio and sync."
    exit 1
  fi
fi
