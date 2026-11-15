# Pleos Journal — Android demo APK

An installable Android shell for the family-journal concept demo. It is a single
fullscreen, landscape, immersive `WebView` Activity that shows the UI from
`../family-journal-v2/` (HTML + PNG + m4a), copied into the APK at build time.

- App label **Pleos Journal**, package `io.pleos.familyjournal.demo`
- minSdk 26, compile/target SDK 35, AGP 8.7.3, Gradle 8.9, Java 17, build-tools 35.0.0
- **No permissions** (no INTERNET, no microphone, no storage). No network content:
  every non-bundled request is answered with an empty 404.
- This is a concept demo. It does not record from the microphone and does not
  integrate with a vehicle; dictation and the voice note are bundled sample clips.

## Build

```sh
./build.sh            # debug APK   -> dist/PleosJournal-debug.apk
./build.sh install    # build + adb install -r
./build.sh release    # release variant, signed with the debug key (demo only)
./build.sh clean
```

`build.sh` locates JDK 17 (`~/Library/Java/temurin-17.jdk/Contents/Home`, or
`JAVA_HOME`) and the SDK (`~/Library/Android/sdk`, or `ANDROID_HOME`), checks
that `platforms/android-35` and `build-tools/35.0.0` exist, and downloads the
official Gradle 8.9 distribution (SHA-256 verified) into `.gradle-dist/` on
first use. The first build also needs network access for the Android Gradle
Plugin from Google Maven.

The build **fails on purpose** if `../family-journal-v2/index.html` is missing.
Use `JOURNAL_DIR=/path/to/ui ./build.sh` to bundle a different UI folder.

### What gets bundled

Gradle task `:app:syncJournalAssets` (runs before every build) copies, never
symlinks, into `app/build/generated/journalAssets/journal/`:

- `index.html`, `timeline.json`
- `assets/**` images (png/jpg/webp/svg), audio/video (m4a/mp3/mp4), json/css/js, fonts

`.aiff`, `.txt`, `.md` and everything else is left out. Media is stored
uncompressed. Rebuild after any UI change; nothing is read from disk at runtime.

## Run

```sh
adb install -r dist/PleosJournal-debug.apk
adb shell am start -n io.pleos.familyjournal.demo/.MainActivity
```

The default launch opens the ordinary interactive home screen, with no autoplay.

The bundled journal UI is interactive-only. Its walkthrough is recorded with actual Android taps. The generic shell extras below are available for future UI implementations; the current UI ignores autoplay and time query parameters.

Optional shell launch extras:

| Extra | Loads |
|---|---|
| `--ez autoplay true` (alias `demo`) | `index.html?autoplay=1&video=1` |
| `--ez autoplay true --ez video false` | `index.html?autoplay=1` |
| `--ef t 42.5` (alias `time`; `--ei`/`--es` also accepted) | `index.html?t=42.5` |
| `--es query "autoplay=1&t=30"` | that exact query string |
| `--ez fileMode true` | force `file:///android_asset/` loading (see below) |

Sending extras to a running instance reloads the page in that mode. A plain
launcher tap brings the journal back exactly as it was.

### Before recording

- Android shows a one-time "Viewing full screen" hint on first immersive launch.
  Suppress it with
  `adb shell settings put secure immersive_mode_confirmations confirmed`
  (already applied to `emulator-5554` during validation).
- To start from an empty journal: `adb shell pm clear io.pleos.familyjournal.demo`
  (or use the in-app reset).
- The screen is kept on while the app is in the foreground.

## How it works

- Pages are served from the APK under a virtual origin,
  `https://appassets.androidplatform.net/journal/`, by `shouldInterceptRequest`.
  That gives the page a secure context, working `fetch()`, and a stable
  `localStorage` origin, without any network or permission. Byte-range requests
  are answered with real `206` responses so audio duration and seeking behave.
- If a WebView build ever refuses that origin, the Activity falls back once to
  `file:///android_asset/journal/`. Note `fetch()` does not work in that mode
  and `localStorage` is a separate store from the default origin.
- JavaScript and DOM storage are on; `localStorage` persists across restarts.
- Audio may start without a user gesture, so playback can also be driven from
  the debug WebView.
- `<input type="file">` opens the system photo picker (multi-select honoured).
  The picker grants per-file access, so no permission is needed.
- Back: the page gets first refusal. It may define `window.onAndroidBack()`
  returning `true` when it handled the press; otherwise the shell taps a visible
  in-app back/close control (`[data-back]`, `[data-action="back"]`,
  `aria-label` containing 뒤로/닫기/취소, `.back`, `.close`). With nothing to go
  back to, a second press within two seconds sends the app to the background.
- Debug builds enable WebView debugging (`chrome://inspect`) and mirror the
  page console to logcat: `adb logcat -s PleosJournal`.
- System font scale and force-dark are ignored so the artboard renders as designed.

## Layout

```
android-demo/
  build.sh                     build / install entry point
  settings.gradle, build.gradle, gradle.properties
  app/build.gradle             SDK levels + syncJournalAssets
  app/src/main/AndroidManifest.xml
  app/src/main/java/io/pleos/familyjournal/demo/MainActivity.java
  app/src/main/res/            label, theme, adaptive launcher icon
  dist/                        build output (created by build.sh)
```
