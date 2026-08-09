#!/usr/bin/env sh
# VortexScape STAGING client launcher (Linux / macOS).
#
# Points at the staging world: game port 43595, client cache over http port 81.
# Keeps its own update channel and its own cache directory, so it can never
# overwrite the live client's jar.

set -eu

DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

JAVA=java
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA="$JAVA_HOME/bin/java"
fi

if ! command -v "$JAVA" >/dev/null 2>&1; then
    echo "Java was not found. Install a Java runtime (17 or newer is fine) and try again." >&2
    exit 1
fi

exec "$JAVA" -jar "$DIR/vortexscape-launcher.jar" \
    staging \
    https://github.com/staticvoid07/Client-Java/releases/download/staging/manifest.json \
    10 1 highmem members 32 staging.vortexidle.com
