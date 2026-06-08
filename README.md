# 재회 매니저

재회 매니저는 카카오톡 대화 내보내기 파일을 가져와 대화를 기기 안에서 정리하고, 연락 가능 여부와 다음 행동, 첫 문장 또는 보류 판단을 만들어 주는 로컬 우선 Android 앱입니다.

## 현재 상태

이 저장소는 단일 Android 앱 모듈로 구성되어 있으며, 현재 다음 흐름을 지원합니다.

- 카카오톡 대화 내보내기 파일 가져오기 (`.txt` 또는 지원되는 `.csv` 계열 텍스트)
- 대화 내용 로컬 파싱
- 대화, 참여자, 메시지, 분석 결과, 로컬 AI 설정을 Room에 저장
- 저장한 대화 목록과 최근 메시지 확인
- 저장한 대화와 정리 결과를 기기에서 삭제
- 연락 판단, 오늘 할 일, 첫 연락 문장 또는 보류 행동으로 구성된 다음 행동 정리
- 내 카톡 이름을 저장하면 새 연락인지 상대 메시지에 대한 답장인지 구분
- Gemma 모델 파일이 없을 때도 전체 흐름을 확인할 수 있는 데모 분석 모드
- Gemma 4 `.litertlm` 모델 파일을 선택하고 실행 점검을 통과하면 LiteRT-LM으로 기기 내 분석 실행

## 제품 원칙

- 가져온 대화와 분석 결과는 기기 안에만 저장합니다.
- 필요 없어진 대화와 분석 결과는 기기에서 삭제할 수 있습니다.
- 로그인, 로그아웃, 회원 관리 기능은 넣지 않습니다.
- 클라우드 동기화, 분석 추적, 원격 업로드, 내장 비밀키는 사용하지 않습니다.
- 앱 모듈은 하나로 유지합니다.
- 내부 패키지는 `ui`, `domain`, `data` 경계를 유지합니다.

상세 범위는 [docs/2026-04-06-mvp-scope.md](docs/2026-04-06-mvp-scope.md)에 정리되어 있습니다.

## 지원하는 카카오톡 가져오기 형식

현재 파서는 실제 공개 자료에서 확인되는 모바일 텍스트, PC CSV, 익명 말뭉치형 텍스트 패턴을 지원합니다.

- 상단의 대화방 제목 또는 헤더
- `저장한 날짜 : yyyy-MM-dd HH:mm:ss`
- `--------------- 2024년 3월 27일 수요일 ---------------` 같은 날짜 구분선
- `[이름] [오전 10:55] 메시지` 형태의 모바일 텍스트 메시지
- 이전 메시지에 이어지는 여러 줄 메시지
- `Date,User,Message` 형태의 PC CSV 계열 행
- `2019-11-04 22:25:00 , P1 : 메시지` 형태의 익명 말뭉치형 행

지원하지 않는 파일은 일부만 저장하지 않고 명확한 가져오기 오류를 보여줍니다.

## 분석 동작

- 앱은 분석 전에 초반 대화, 최근 대화, 감정/경계 신호 주변 메시지, 긴 공백과 마지막 발신자 같은 지표를 먼저 추립니다.
- 내 카톡 이름이 설정되어 있으면 마지막 발신자가 나인지 상대인지 구분해 보류와 답장 제안을 다르게 냅니다.
- Gemma 4 모델이 없거나 실행 점검 전이면 데모 분석 provider를 사용합니다.
- Gemma 4 `.litertlm` 파일을 선택한 뒤 앱 안의 모델 실행 점검을 통과해야 LiteRT-LM provider가 실제 분석에 사용됩니다.
- 검증된 모델 분석은 로컬 병렬 검수 provider가 안전, 마지막 메시지, 관계 맥락을 다시 확인한 뒤 저장합니다.
- 모델 파일은 APK에 포함하지 않습니다. 앱의 로컬 AI 설정에서 `gemma-4-E4B-it.litertlm` 같은 모델 파일을 선택하면 앱 전용 저장소로 복사합니다.
- 분석 결과는 로컬에 저장되며, 확정적인 판단 대신 `지금은 보류`, `먼저 사과 필요`, `아주 가볍게 가능`, `정보 부족` 같은 조심스러운 연락 판단으로 표시됩니다. 보류 판단에서는 보낼 문장을 만들지 않고 오늘 보내지 않는 행동을 제안합니다.

## 로컬 검증

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

에뮬레이터가 준비되어 있다면 사용자 흐름까지 확인할 수 있습니다.

```bash
./gradlew connectedDebugAndroidTest
```

실제 `.litertlm` 모델 파일까지 확인하려면 모델을 기기에 올린 뒤 smoke test에 경로를 넘깁니다. 에뮬레이터에서는 LiteRT-LM 네이티브 런타임이 `SIGILL`로 종료될 수 있으므로, 최종 모델 추론 검증은 실제 ARM64 기기에서 확인해야 합니다.

```bash
adb push /path/to/gemma-4-E4B-it.litertlm /data/local/tmp/gemma-4-E4B-it.litertlm
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.bssm.reunionmanager.data.analysis.Gemma4AnalysisProviderDeviceSmokeTest \
  -Pandroid.testInstrumentationRunnerArguments.gemmaModelPath=/data/local/tmp/gemma-4-E4B-it.litertlm
```

직접 실행하려면 debug APK를 설치한 뒤 앱을 시작합니다.

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.bssm.reunionmanager/.MainActivity
```

## 주요 화면

- 홈 화면
- 카카오톡 대화 가져오기
- 저장한 대화 목록
- 대화 상세 보기
- 연락 판단과 첫 연락 문장 또는 보류 행동이 포함된 다음 행동
- 로컬 AI 설정

## 프로젝트 구조

- `app/src/main/java/com/bssm/reunionmanager/ui` - Compose 화면, 내비게이션, ViewModel 상태
- `app/src/main/java/com/bssm/reunionmanager/domain` - use case와 분석 provider 계약
- `app/src/main/java/com/bssm/reunionmanager/data` - 파서, Room 저장소, repository, provider 구현
- `docs/` - 제품 범위와 구현 문서
- `.github/workflows/android.yml` - 유닛 테스트, lint, debug 빌드, 에뮬레이터 기능 테스트

## 문서

- [docs/2026-04-06-mvp-scope.md](docs/2026-04-06-mvp-scope.md) - MVP 범위와 제외 항목
- [docs/2026-04-06-implementation.md](docs/2026-04-06-implementation.md) - 구현 구조와 QA 기록

## CI

GitHub Actions는 다음 검증을 실행합니다.

- `./gradlew testDebugUnitTest`
- `./gradlew lintDebug`
- `./gradlew assembleDebug`
- `./gradlew connectedDebugAndroidTest`

성공한 실행에서는 `app/build/outputs/apk/debug/app-debug.apk`가 workflow artifact로 업로드됩니다.

## 릴리스

`v*` 태그를 push하면 Android release workflow가 실행됩니다. 이 workflow는 debug APK를 빌드하고 artifact로 업로드한 뒤, 같은 APK를 GitHub Release에 첨부합니다.
