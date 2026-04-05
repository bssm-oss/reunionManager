# AGENTS

## Repository Intent

This repository hosts the Reunion Manager Android MVP.

## Current Constraints

- Keep a single Android app module unless scope changes explicitly.
- Follow `docs/2026-04-06-mvp-scope.md` before adding new product behavior.
- Preserve the internal `ui`, `domain`, and `data` package split.
- Keep storage local-only for MVP work.
- Do not add auth, cloud sync, analytics, or secrets.

## Implemented MVP Behavior

- Import a KakaoTalk `.txt` export from local storage.
- Parse supported KakaoTalk date/message lines into local entities.
- Save conversations, participants, messages, analysis results, and provider settings in Room.
- Browse saved conversations and inspect message history.
- Generate a reunion plan through:
  - a fake local provider when no Gemini API key exists
  - a Gemini-compatible HTTP provider when the user saves local settings

## Editing Rules

- Preserve the fake-provider fallback when provider settings are empty.
- Preserve clear local-only copy in the UI.
- Do not weaken duplicate import protection based on source hash.
- Do not widen the supported KakaoTalk format without matching parser tests.
- Keep new provider behavior behind the existing provider abstraction.

## Test Expectations

- Parser changes must update parser fixture/unit tests.
- Persistence changes must keep Room-backed repository tests passing.
- Analysis changes must keep the missing-config fallback path covered.

## Validation Contract

Before considering work complete, run:

1. `./gradlew testDebugUnitTest`
2. `./gradlew lintDebug`
3. `./gradlew assembleDebug`

If emulator/manual QA is possible, also install and launch the debug APK.
