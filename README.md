# Reunion Manager

Reunion Manager is a local-first Android MVP for importing a KakaoTalk conversation export, reviewing the saved chat on-device, and generating a structured reunion plan.

## MVP Status

This repository now contains a working single-module Android app with the following end-to-end MVP flow:

- import a KakaoTalk plain-text export (`.txt`)
- parse the conversation locally
- store the conversation, participants, messages, analysis results, and provider settings on-device with Room
- browse saved conversations and inspect recent messages
- generate a structured reunion plan
- use a fake local analysis provider when Gemini is not configured
- optionally call a Gemini-compatible endpoint when the user provides local settings

## Product Constraints

- local storage only for MVP data
- no login, logout, or member management
- no cloud sync, analytics, or bundled secrets
- single Android app module
- internal `ui`, `domain`, and `data` package split

The canonical scope contract lives in `docs/2026-04-06-mvp-scope.md`.

## Supported KakaoTalk Import Format

The MVP supports KakaoTalk plain-text exports whose structure matches the observed desktop/mobile export pattern:

- title/header line near the top
- `저장한 날짜 : yyyy-MM-dd HH:mm:ss`
- date divider lines like `--------------- 2024년 3월 27일 수요일 ---------------`
- message lines like `[이름] [오전 10:55] 메시지`
- multiline continuations appended to the prior message

If a selected file does not match the supported format, the app fails with a clear local error instead of saving partial data.

## Analysis Behavior

- if no Gemini API key is stored locally, the app uses a fake local provider so the full workflow remains usable
- if a Gemini-compatible API key, model, and endpoint are stored locally, the app can call the configured provider
- generated results are stored locally and shown as a structured plan with bounded, non-certain language

## Local Validation

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

## Optional Emulator QA

If you have a local emulator configured, you can launch the app after building:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.bssm.reunionmanager/.MainActivity
```

The automated feature-test lane is intended to cover the same core MVP flow through instrumentation: home trust signals, import navigation, settings navigation, and browsing an imported conversation through reunion-plan generation.

## Project Structure

- `app/src/main/java/com/bssm/reunionmanager/ui` — Compose screens, navigation, and shared view-model state
- `app/src/main/java/com/bssm/reunionmanager/domain` — provider contract and MVP use cases
- `app/src/main/java/com/bssm/reunionmanager/data` — parser, Room persistence, repositories, and provider implementations
- `docs/` — product scope and implementation documentation
- `.github/workflows/android.yml` — CI for unit tests, lint, debug assembly, and emulator-backed feature tests

## Key Screens

- home
- import
- saved conversation list
- conversation detail
- reunion plan
- AI settings

## Documentation

- `docs/2026-04-06-mvp-scope.md` — locked MVP scope and deferred items
- `docs/2026-04-06-implementation.md` — implementation details, architecture, and QA notes

## CI

GitHub Actions runs the same core validation commands used locally:

- `./gradlew testDebugUnitTest`
- `./gradlew lintDebug`
- `./gradlew assembleDebug`
- `./gradlew connectedDebugAndroidTest`

The CI workflow also uploads `app/build/outputs/apk/debug/app-debug.apk` as a workflow artifact so each successful run keeps the built debug APK.

## Releases

Pushing a `v*` tag triggers the Android release workflow. That workflow builds `app/build/outputs/apk/debug/app-debug.apk`, uploads it as a workflow artifact, and attaches the same installable APK to the GitHub Release for that tag.
