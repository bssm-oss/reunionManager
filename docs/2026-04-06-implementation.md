# Reunion Manager Implementation Notes

## Current Vertical Slice

The current MVP implementation supports one grounded local-first flow:

1. The user selects a KakaoTalk `.txt` export from device storage.
2. The app parses the supported export structure.
3. The app stores the imported conversation locally in Room.
4. The user opens the saved conversation list and a conversation detail view.
5. The user generates a reunion plan.
6. The result is saved locally and shown in the analysis screen.

## Architecture

### `ui`

- `MainViewModel` coordinates import, analysis generation, and settings updates.
- Navigation is single-activity Compose navigation.
- UI screens are intentionally simple and use plain language around privacy and uncertainty.

### `domain`

- `AnalysisProvider` is the provider boundary.
- `ImportConversationUseCase` parses and stores a selected file.
- `GenerateReunionPlanUseCase` chooses the active provider and persists the result.

### `data`

- `KakaoTalkConversationParser` parses the supported export format.
- Room stores:
  - conversations
  - participants
  - messages
  - analysis results
  - provider settings
- `FakeAnalysisProvider` keeps the MVP workflow available without remote configuration.
- `GeminiAnalysisProvider` calls a Gemini-compatible HTTP endpoint only when the user supplies local settings.

## Import Rules

- duplicate protection uses a SHA-256 hash of the raw imported text
- multiline text after a recognized message line is appended to the prior message
- unsupported files raise a local import error and are not partially stored

## Provider Rules

- blank API key => fake provider
- configured API key => Gemini-compatible provider
- generated analysis is stored locally either way

## Test Coverage

Current automated coverage focuses on the highest-risk behaviors:

- parser behavior for supported, multiline, and unsupported KakaoTalk text
- Room-backed conversation import and duplicate handling
- fake-provider behavior
- analysis fallback behavior when no Gemini key is configured
- instrumentation feature tests for:
  - home trust signals
  - import navigation
  - settings navigation
  - imported conversation browsing through reunion-plan generation

## QA Notes

Primary local validation commands:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew connectedDebugAndroidTest
```

The connected Android test lane is intended to exercise the core MVP user-visible flow:

- home trust signals
- import navigation
- settings navigation
- browsing an imported conversation through reunion-plan generation

Optional device/emulator launch:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.bssm.reunionmanager/.MainActivity
```

## Known MVP Limits

- only KakaoTalk `.txt` import is supported
- no account system or sync
- no background upload behavior
- no multi-provider marketplace
- no advanced analytics or dashboards

## GitHub Delivery Notes

The final repository state was delivered through a repaired PR workflow:

- PR #1 delivered the initial MVP branch and was self-merged.
- PR #2 reverted a direct-to-main follow-up fix after Oracle flagged the workflow mismatch.
- PR #3 reapplied the same final fixes through the required branch -> PR -> self-merge path.
- PR #4 aligned the implementation notes with the final delivered state.
- PR #5 stabilized the imported-chat analysis instrumentation wait so the final feature-test lane stayed green on CI.
- PR #6 aligned the README and implementation notes with the final shipped CI scope and repaired PR history.

That leaves the current `main` state aligned with both the requested implementation and the requested delivery process.

## GitHub Automation

- `.github/workflows/android.yml` runs `testDebugUnitTest`, `lintDebug`, `assembleDebug`, and `connectedDebugAndroidTest`
- the same CI workflow uploads `app/build/outputs/apk/debug/app-debug.apk` as a workflow artifact
- `.github/workflows/release.yml` runs on `v*` tags, builds `app/build/outputs/apk/debug/app-debug.apk`, uploads it as a workflow artifact, and attaches it to the matching GitHub Release
