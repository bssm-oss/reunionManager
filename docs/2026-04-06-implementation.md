# Reunion Manager Implementation Notes

## Current Vertical Slice

The current MVP implementation supports one grounded local-first flow:

1. The user selects a KakaoTalk `.txt` or supported `.csv`-style export from device storage.
2. The app parses the supported export structure.
3. The app stores the imported conversation locally in Room.
4. The user opens the saved conversation list and a conversation detail view.
5. The user generates a reunion plan.
6. The app extracts early messages, recent messages, emotion/boundary signal windows, conversation stats, and user/counterpart perspective before calling the active provider.
7. The result is saved locally and shown as a compact Korean action plan with contact readiness, evidence, a next step, a first-contact message draft or no-send action, and caution.

## Architecture

### `ui`

- `MainViewModel` coordinates import, analysis generation, and settings updates.
- Navigation is single-activity Compose navigation.
- UI screens are intentionally simple, Korean-first, and use plain language around privacy and uncertainty.

### `domain`

- `AnalysisProvider` is the provider boundary.
- `ImportConversationUseCase` parses and stores a selected file.
- `GenerateReunionPlanUseCase` chooses the active provider and persists the result.
- `ConversationRepository.buildAnalysisInput` builds a compact evidence packet from opening messages, recent messages, signal windows, and stats such as last sender and long gaps.
- When the optional local user display name is set, the analysis input also marks whether the last sender is the user or the counterpart.

### `data`

- `KakaoTalkConversationParser` parses the supported export format.
- Room stores:
  - conversations
  - participants
  - messages
  - analysis results
  - provider settings
  - optional local user display name
- `FakeAnalysisProvider` keeps the MVP workflow available without remote configuration.
- `Gemma4AnalysisProvider` runs a verified Gemma 4 `.litertlm` model on-device through LiteRT-LM.
- `LocalAnalysisSwarmProvider` blends the verified Gemma draft with local safety, last-message, context, and baseline checks before saving a result.

## Import Rules

- duplicate protection uses a SHA-256 hash of the imported text after stripping a leading UTF-8 BOM marker
- multiline text after a recognized message line is appended to the prior message
- PC CSV-style rows with date/user/message columns are supported
- anonymized corpus-style rows like `YYYY-MM-DD HH:mm:ss , P1 : message` are supported
- unsupported files raise a local import error and are not partially stored

## Provider Rules

- blank model path or unchecked model file => fake provider
- verified model path, backend, and execution timestamp => Gemma 4 on-device provider behind the local swarm provider
- model settings can copy a selected `.litertlm` file into app-private storage before saving its filesystem path, but analysis keeps using the fake provider until the smoke check succeeds
- generated analysis is stored locally either way
- generated analysis includes a `messageDraft` field so the user has either a concrete first-contact sentence or a clear no-send action, not just abstract advice
- generated analysis includes contact readiness, evidence, and alternative first-contact candidates so a small local model is less dependent on generic advice
- generated analysis uses the local user display name when available so unanswered user messages and counterpart reply opportunities do not collapse into the same advice

## Test Coverage

Current automated coverage focuses on the highest-risk behaviors:

- parser behavior for supported, multiline, and unsupported KakaoTalk text
- Room-backed conversation import and duplicate handling
- fake-provider behavior
- analysis fallback behavior when no Gemma model path is configured or a copied model has not passed execution check
- persisted first-contact draft behavior
- analysis input extraction for recent messages, signal windows, and stats
- deterministic fake-provider judgments for boundary waiting, unanswered-message waiting, apology-first, and light-contact scenarios
- perspective-aware tests for user final-message runs and counterpart final-message reply opportunities
- instrumentation feature tests for:
  - home trust signals
  - import navigation
  - settings navigation
  - unchecked model files staying in demo analysis
  - imported conversation browsing through reunion-plan generation and first-message display

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
- first-contact draft display

Optional device/emulator launch:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.bssm.reunionmanager/.MainActivity
```

## Known MVP Limits

- imports are limited to KakaoTalk mobile text, PC CSV-style, and anonymized corpus-style text exports
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
