#!/usr/bin/env bash
# Builds the Pleos Journal demo APK.
#   ./build.sh                 debug APK  -> dist/PleosJournal-debug.apk
#   ./build.sh release         release APK (debug-signed) -> dist/PleosJournal-release.apk
#   ./build.sh install         build debug + adb install -r
#   ./build.sh clean
# Env overrides: JAVA_HOME, ANDROID_HOME / ANDROID_SDK_ROOT, JOURNAL_DIR
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$HERE"

GRADLE_VERSION="8.9"
GRADLE_SHA256="d725d707bfabd4dfdc958c624003b3c80accc03f7037b5122c4b1d0ef15cecab"
GRADLE_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
DIST_DIR="$HERE/.gradle-dist"

say()  { printf '\033[1;36m[pleos-journal]\033[0m %s\n' "$*"; }
fail() { printf '\033[1;31m[pleos-journal] ERROR:\033[0m %s\n' "$*" >&2; exit 1; }

# ---------------------------------------------------------------- JDK 17
is_jdk17() {
  [ -x "$1/bin/java" ] && "$1/bin/java" -version 2>&1 | grep -Eq 'version "17(\.|")'
}
find_jdk() {
  local c
  for c in \
    "${JAVA_HOME:-}" \
    "$HOME/Library/Java/temurin-17.jdk/Contents/Home" \
    "$HOME/Library/Java/temurin-17.jdk" \
    "$HOME"/Library/Java/jdk-17*/Contents/Home \
    "$HOME"/Library/Java/JavaVirtualMachines/*17*/Contents/Home \
    /Library/Java/JavaVirtualMachines/*17*/Contents/Home \
    /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
    /usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
    /usr/lib/jvm/*17*; do
    [ -n "$c" ] && is_jdk17 "$c" && { echo "$c"; return 0; }
  done
  if [ -x /usr/libexec/java_home ]; then
    c="$(/usr/libexec/java_home -v 17 2>/dev/null || true)"
    [ -n "$c" ] && is_jdk17 "$c" && { echo "$c"; return 0; }
  fi
  return 1
}
JAVA_HOME="$(find_jdk)" || fail "JDK 17 not found. Expected $HOME/Library/Java/temurin-17.jdk/Contents/Home (or set JAVA_HOME)."
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"
say "JDK   : $JAVA_HOME"

# ---------------------------------------------------------------- Android SDK
find_sdk() {
  local c
  for c in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}" "$HOME/Library/Android/sdk" "$HOME/Android/Sdk"; do
    [ -n "$c" ] && [ -d "$c" ] && { echo "$c"; return 0; }
  done
  return 1
}
SDK="$(find_sdk)" || fail "Android SDK not found. Expected $HOME/Library/Android/sdk (or set ANDROID_HOME)."
export ANDROID_HOME="$SDK" ANDROID_SDK_ROOT="$SDK"
say "SDK   : $SDK"

MODE="${1:-debug}"

if [ "$MODE" != "clean" ]; then
  [ -f "$SDK/platforms/android-35/android.jar" ] || fail "SDK platform android-35 is missing in $SDK/platforms (still installing?)."
  [ -x "$SDK/build-tools/35.0.0/aapt2" ]         || fail "SDK build-tools 35.0.0 is missing in $SDK/build-tools (still installing?)."
  JOURNAL="${JOURNAL_DIR:-$HERE/../family-journal-v2}"
  [ -f "$JOURNAL/index.html" ] || fail "Journal UI not ready: $JOURNAL/index.html does not exist."
  JOURNAL="$(cd "$JOURNAL" && pwd)"
  say "UI    : $JOURNAL"
fi

# ---------------------------------------------------------------- Gradle 8.9
if [ -x "$HERE/gradlew" ] && [ -f "$HERE/gradle/wrapper/gradle-wrapper.jar" ]; then
  GRADLE=("$HERE/gradlew")
else
  GRADLE_HOME_DIR="$DIST_DIR/gradle-${GRADLE_VERSION}"
  if [ ! -x "$GRADLE_HOME_DIR/bin/gradle" ]; then
    say "Fetching official Gradle ${GRADLE_VERSION} distribution"
    mkdir -p "$DIST_DIR"
    ZIP="$DIST_DIR/gradle-${GRADLE_VERSION}-bin.zip"
    curl -fL --retry 3 --retry-delay 2 -o "$ZIP.part" "$GRADLE_URL"
    GOT="$(shasum -a 256 "$ZIP.part" | awk '{print $1}')"
    [ "$GOT" = "$GRADLE_SHA256" ] || { rm -f "$ZIP.part"; fail "Gradle checksum mismatch (got $GOT)."; }
    mv "$ZIP.part" "$ZIP"
    unzip -q -o "$ZIP" -d "$DIST_DIR"
    rm -f "$ZIP"
  fi
  GRADLE=("$GRADLE_HOME_DIR/bin/gradle")
fi
say "Gradle: ${GRADLE[0]}"

ARGS=(--project-dir "$HERE" --console=plain --no-daemon)
[ -n "${JOURNAL:-}" ] && ARGS+=("-PjournalDir=$JOURNAL")

publish() { # $1 = variant
  local src="$HERE/app/build/outputs/apk/$1/app-$1.apk"
  [ -f "$src" ] || fail "Expected APK not produced: $src"
  mkdir -p "$HERE/dist"
  cp -f "$src" "$HERE/dist/PleosJournal-$1.apk"
  say "APK   : $HERE/dist/PleosJournal-$1.apk ($(du -h "$HERE/dist/PleosJournal-$1.apk" | awk '{print $1}'))"
}

case "$MODE" in
  clean)
    "${GRADLE[@]}" "${ARGS[@]}" clean
    rm -rf "$HERE/dist"
    ;;
  debug)
    "${GRADLE[@]}" "${ARGS[@]}" :app:assembleDebug
    publish debug
    ;;
  release)
    "${GRADLE[@]}" "${ARGS[@]}" :app:assembleRelease
    publish release
    ;;
  install)
    "${GRADLE[@]}" "${ARGS[@]}" :app:assembleDebug
    publish debug
    "$SDK/platform-tools/adb" install -r "$HERE/dist/PleosJournal-debug.apk"
    say "Launch: adb shell am start -n io.pleos.familyjournal.demo/.MainActivity"
    ;;
  *)
    fail "Unknown mode '$MODE' (use: debug | release | install | clean)"
    ;;
esac
