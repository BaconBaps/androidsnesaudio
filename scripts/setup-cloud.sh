#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
PIXELSPC_TOOLS="${PIXELSPC_TOOLS:-/workspace/tooling}"
mkdir -p "$PIXELSPC_TOOLS" "$PIXELSPC_TOOLS/android-user/cache" "$PIXELSPC_TOOLS/gradle-home"
fetch() {
  local url="$1" path="$2" checksum="$3"
  if [ ! -f "$path" ]; then curl --fail --location --retry 2 "$url" -o "$path"; fi
  printf '%s  %s\n' "$checksum" "$path" | sha256sum --check --status
}
if [ ! -x "$PIXELSPC_TOOLS/jdk-21.0.8+9/bin/javac" ]; then
  fetch 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.8%2B9/OpenJDK21U-jdk_x64_linux_hotspot_21.0.8_9.tar.gz' "$PIXELSPC_TOOLS/jdk.tar.gz" f2dc5418092c43003db8f9005c4a286e1c0104fea96ccdd49e8ebd037cac9219
  tar -xzf "$PIXELSPC_TOOLS/jdk.tar.gz" -C "$PIXELSPC_TOOLS"
fi
if [ ! -x "$PIXELSPC_TOOLS/android-sdk/cmdline-tools/latest/bin/sdkmanager" ]; then
  fetch 'https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip' "$PIXELSPC_TOOLS/cmdtools.zip" 2d2d50857e4eb553af5a6dc3ad507a17adf43d115264b1afc116f95c92e5e258
  mkdir -p "$PIXELSPC_TOOLS/android-sdk/cmdline-tools"
  unzip -q "$PIXELSPC_TOOLS/cmdtools.zip" -d "$PIXELSPC_TOOLS/android-sdk/cmdline-tools"
  mv "$PIXELSPC_TOOLS/android-sdk/cmdline-tools/cmdline-tools" "$PIXELSPC_TOOLS/android-sdk/cmdline-tools/latest"
fi
# Retain the JDK's public roots and add the managed environment's trusted CAs.
if [ ! -f "$PIXELSPC_TOOLS/cacerts" ]; then
  cp "$PIXELSPC_TOOLS/jdk-21.0.8+9/lib/security/cacerts" "$PIXELSPC_TOOLS/cacerts"
  for cert in /usr/local/share/ca-certificates/*.crt; do
    [ -f "$cert" ] || continue
    "$PIXELSPC_TOOLS/jdk-21.0.8+9/bin/keytool" -importcert -noprompt -alias "pixelspc-$(basename "$cert")" -file "$cert" -keystore "$PIXELSPC_TOOLS/cacerts" -storepass changeit
  done
fi
source scripts/cloud-env.sh
SDK_PROXY=()
if [ -n "${PIXELSPC_PROXY_HOST:-}" ]; then SDK_PROXY=(--proxy=http "--proxy_host=$PIXELSPC_PROXY_HOST" "--proxy_port=$PIXELSPC_PROXY_PORT"); fi
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Djavax.net.ssl.trustStore=$PIXELSPC_TOOLS/cacerts"
# Avoid pipefail treating yes's expected SIGPIPE as an SDK error.
set +o pipefail
yes | sdkmanager "${SDK_PROXY[@]}" --licenses
SDK_LICENSE_STATUS=${PIPESTATUS[1]}
set -o pipefail
[ "$SDK_LICENSE_STATUS" -eq 0 ]
sdkmanager "${SDK_PROXY[@]}" 'platforms;android-35' 'build-tools;35.0.0' 'ndk;27.2.12479018' 'cmake;3.22.1' 'platform-tools'
scripts/build.sh
