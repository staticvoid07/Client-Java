#!/usr/bin/env sh
# VortexScape client launcher (Linux / macOS).
#
# Args after the jar are:  <channel> <manifestUrl> <client args...>
# The client args are the same ones the Windows build uses:
#   nodeid portOffset memory members ? host
#
# portOffset 0 -> game port 43594, cache over http on port 80
# portOffset 1 -> game port 43595, cache over http on port 81   (staging)

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

exec "$JAVA" -XX:+IgnoreUnrecognizedVMOptions -Xmx2g -XX:G1PeriodicGCInterval=60000 -XX:+G1PeriodicGCInvokesConcurrent -jar "$DIR/vortexscape-launcher.jar" \
    live \
    https://github.com/staticvoid07/Client-Java/releases/download/live/manifest.json \
    10 0 highmem members 32 play.vortexidle.com
