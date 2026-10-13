# CLAUDE.md

Guidance for Claude Code working in this repository.

## What this is

**Keepsake** — an Android-only app that lets a user browse the WhatsApp status images and videos
already cached on their phone, preview them, and save chosen ones into the phone's
local gallery. Simple and hassle free: no account, no network, no ads, no analytics.
Light and dark themes follow the system setting by default.

## Start of every session (mandatory)

The user compacts conversations often. Conversation memory is not reliable; the
trackers are. Before doing any work:

1. Read `docs/tracker/STATUS.md` — the "Resume here" block says what is in progress
   and what is next.
2. Skim the latest entries in `docs/tracker/SESSION_LOG.md`.
3. Check `docs/tracker/DECISIONS.md` before re-opening any settled question.
4. Read the section of `docs/plan.md` for the current milestone.

## Keeping trackers current (mandatory)

- When starting a task: set it to `WIP` in `STATUS.md` and update "Resume here".
- When finishing a task: set it to `DONE`, record how it was verified, and point
  "Resume here" at the next task.
- When blocked: set `BLOCKED` with the reason and what is needed to unblock.
- Any non-obvious choice (library, API, storage path, scope cut): add an entry to
  `DECISIONS.md`.
- Before the conversation is likely to be compacted, or at the end of a working
  block: append a dated entry to `SESSION_LOG.md`.
- Trackers are the source of truth for progress. Never leave them stale after a
  code change.

## Working agreement with the user

- One task at a time. Finish the task, verify it, update the trackers, then report
  and stop. Do not commit or start the next task until the user says so (usually
  "commit and start T<next>").
- One commit per task on `main`, Conventional Commits, task ID in the subject.
- Report each finished task as Theory / Verified / Risk, then the list of files
  created or modified with one line each. No code diffs in the report.
- Settle anything that is the user's call (scope, naming, distribution) with a
  question before writing it into the docs.
- Look up library and tool versions from the live repositories (Gradle services,
  Google Maven, Maven Central `maven-metadata.xml`) rather than from memory.

## Documents

| File | Purpose |
|------|---------|
| `docs/spec.md` | Product spec: what the app does, for whom, and what is out of scope |
| `docs/design.md` | Program theory and technical design: architecture, storage access, invariants |
| `docs/plan.md` | Milestones and tasks with acceptance criteria |
| `docs/tracker/STATUS.md` | Live task status board and "Resume here" pointer |
| `docs/tracker/DECISIONS.md` | Decision log (ADR-style, append only) |
| `docs/tracker/SESSION_LOG.md` | Dated per-session log of what was done |

## How to approach code work

Use the `programming-as-theory-building` skill for any non-trivial code change.
Before coding, answer briefly: world mapping, current theory, similarity, boundary,
proof (see `docs/design.md` section 1 for the program theory). End non-trivial work
with the Theory / Changed / Verified / Risk summary and copy the key line into the
task's notes in `STATUS.md`.

If a change contradicts `docs/design.md`, either the change is wrong or the design
must be updated in the same change, with a `DECISIONS.md` entry.

Show that new tests can fail: break the code briefly (a mutation such as flipping a
condition), confirm the expected tests fail, then restore it. Report which mutations
were caught and which survived; a surviving mutation is a gap to close or to record in
"Pending checks" in `STATUS.md`. Do not commit placeholder tests.

## Tech stack

- Kotlin, Jetpack Compose, Material 3 (dynamic color on Android 12+, system light/dark).
- minSdk 26 (Android 8.0), targetSdk / compileSdk: latest stable.
- Single Gradle module `:app`, Kotlin DSL, version catalog `gradle/libs.versions.toml`.
- Gradle 9 wrapper, AGP 9 with built-in Kotlin: do not apply `org.jetbrains.kotlin.android`.
  Exact versions live in the version catalog and DECISIONS D-010.
- MVVM: `ViewModel` + `StateFlow`; no DI framework (manual wiring in `Application`).
- Coil 3 (with video frame decoder) for thumbnails; Media3 ExoPlayer for video.
- Storage Access Framework (SAF) to read statuses; MediaStore to save to gallery.
- DataStore (Preferences) for the persisted folder URI and theme preference.

## Hard rules (invariants)

- No `INTERNET` permission. The app never touches the network.
- Read statuses only through the SAF tree URI the user granted. No
  `MANAGE_EXTERNAL_STORAGE`, no `READ_MEDIA_*` or `READ_EXTERNAL_STORAGE` for reading.
- Saved files go to the shared gallery via MediaStore under `Pictures/Keepsake`
  and `Movies/Keepsake`. `WRITE_EXTERNAL_STORAGE` only with `maxSdkVersion="28"`.
- Never modify or delete anything inside WhatsApp's folders.
- Media3's `ACCESS_NETWORK_STATE` and `WAKE_LOCK` are removed in the manifest (D-011):
  never call `ExoPlayer.setWakeMode()`.
- Do not use "WhatsApp" in the app name or launcher label (trademark).
- Keep it simple: no feature, setting, or abstraction without a current need in
  `docs/spec.md`.

## Commands

Requires JDK 17+ (JDK 21 used) and the Android SDK; set `sdk.dir` in
`local.properties` (git-ignored) or `ANDROID_HOME`.

```bash
./gradlew assembleDebug          # build debug APK
./gradlew installDebug           # install on connected device/emulator
./gradlew testDebugUnitTest      # JVM unit tests
./gradlew connectedDebugAndroidTest  # instrumented tests (device required)
./gradlew lintDebug              # Android lint
```

Device checks use the local emulator `keepsake_api35` (API 35, config under
`~/.android/avd/`). Always target it by serial, because a physical phone may also be
attached; do not install on or change settings of the user's phone without asking.

```bash
~/Android/Sdk/emulator/emulator -avd keepsake_api35 -no-window -no-audio -no-snapshot -gpu host &
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell cmd uimode night yes   # or "no": flip system dark mode
adb -s emulator-5554 exec-out screencap -p > screen.png
adb -s emulator-5554 emu kill
```

Useful for device checks (the emulator has no WhatsApp):

```bash
# Fake statuses folders (mkdir works under Android/media; touch there does not).
adb -s emulator-5554 shell 'mkdir -p "/sdcard/Android/media/com.whatsapp/WhatsApp/Media/.Statuses"'
# Fake statuses: adb push works there; -a keeps the local modified time (set it with touch -d).
# ffmpeg is installed for a tiny test video: ffmpeg -f lavfi -i color=c=red:s=64x64:d=1 -pix_fmt yuv420p v.mp4
adb -s emulator-5554 push -a w1.png v.mp4 "/sdcard/Android/media/com.whatsapp/WhatsApp/Media/.Statuses/"
adb -s emulator-5554 shell pm clear com.moorthy.statussaver            # fresh-install state, drops grants
adb -s emulator-5554 shell run-as com.moorthy.statussaver cat files/datastore/status_sources.preferences_pb | strings
adb -s emulator-5554 shell dumpsys activity permissions | grep -B4 "persisted=0x1"   # persisted SAF grants
adb -s emulator-5554 shell uiautomator dump /sdcard/ui.xml            # then tap a node's bounds centre
adb -s emulator-5554 shell input tap <x> <y>
```

Use `-gpu host` for anything about smoothness: without it a headless emulator renders
with SwiftShader on the CPU and even a plain grid janks. Measure with
`adb -s emulator-5554 shell dumpsys gfxinfo com.moorthy.statussaver` (`reset` first,
then scripted `input swipe`); compare against Settings as a baseline.

The folder picker's buttons read "USE THIS FOLDER" and "ALLOW"; it hides `.Statuses`
unless "Show hidden files" is on in its menu. A cold boot may show "System UI isn't
responding": tap Wait.

Update this section if commands change.

## Conventions

- Package root: `com.moorthy.statussaver` (change only via a `DECISIONS.md` entry).
- Packages by layer inside the single module: `data/`, `ui/<screen>/`, `ui/theme/`.
- Commit messages: Conventional Commits, normal English.
- One task per commit where practical; reference the task ID (e.g. `T2.3`).
