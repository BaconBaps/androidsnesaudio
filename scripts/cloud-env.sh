#!/usr/bin/env bash
# Source this in the managed cloud environment; standard local SDKs also work.
PIXELSPC_TOOLS="${PIXELSPC_TOOLS:-/workspace/tooling}"
if [ -d "$PIXELSPC_TOOLS/jdk-21.0.8+9" ]; then
  export JAVA_HOME="$PIXELSPC_TOOLS/jdk-21.0.8+9"
  export PATH="$JAVA_HOME/bin:$PATH"
fi
if [ -d "$PIXELSPC_TOOLS/android-sdk" ]; then
  export ANDROID_HOME="$PIXELSPC_TOOLS/android-sdk"
  export ANDROID_USER_HOME="$PIXELSPC_TOOLS/android-user"
  export GRADLE_USER_HOME="$PIXELSPC_TOOLS/gradle-home"
  export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
fi
# Java does not automatically inherit the session's HTTPS proxy or curl CA trust.
# Pass host/port only; do not log proxy URLs, credentials, or the environment.
PIXELSPC_JAVA_ARGS=()
if [ -n "${HTTPS_PROXY:-}" ]; then
  read -r PIXELSPC_PROXY_HOST PIXELSPC_PROXY_PORT < <(python3 -c 'import os,urllib.parse; p=urllib.parse.urlsplit(os.environ["HTTPS_PROXY"]); print(p.hostname,p.port or 80)')
  PIXELSPC_JAVA_ARGS+=("-Dhttps.proxyHost=$PIXELSPC_PROXY_HOST" "-Dhttps.proxyPort=$PIXELSPC_PROXY_PORT" "-Dhttp.proxyHost=$PIXELSPC_PROXY_HOST" "-Dhttp.proxyPort=$PIXELSPC_PROXY_PORT")
fi
if [ -f "$PIXELSPC_TOOLS/cacerts" ]; then
  PIXELSPC_JAVA_ARGS+=("-Djavax.net.ssl.trustStore=$PIXELSPC_TOOLS/cacerts")
fi
