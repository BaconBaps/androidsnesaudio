#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/cloud-env.sh
if [ "$#" -eq 0 ]; then set -- assembleDebug testDebugUnitTest lintDebug; fi
./gradlew --no-daemon "${PIXELSPC_JAVA_ARGS[@]}" "$@"
