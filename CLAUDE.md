# Pleos Journal (가제): Claude Code 작업 지침

이 파일은 Claude Code가 매 세션 시작 시 읽는다. 짧게 유지하고, 상세 내용은 `docs/`에 둔다.

## 1. 프로젝트 한 줄

현대자동차그룹 차량용 앱 개발 공모전(Pleos Playground) 출품작.
**차가 이동 중 "기억의 단서"를 모으고, 정차 후 사용자가 음성 한마디로 의미를 붙이는 드라이브 저널 앱.**
Pleos Connect(Android Automotive 기반) 위에서 동작하는 차량용 Android 앱.

## 2. 팀과 작업 방식

- 2인 팀. 둘 다 자동차 양산 SW 개발 경험이 있고, Android 앱 개발 경험은 적음.
- 앱 코드는 AI-Assisted 코딩으로 작성하고, 구조 설계와 검증은 사람이 맡는다.
- 따라서 Claude는 **Android/Kotlin 관용구를 설명하면서** 작업하되, 신호 처리, 상태 머신, 테스트 설계 같은 임베디드 SW 개념은 짧게 넘어가도 된다.

## 3. 먼저 읽을 문서 (작업 전 해당 문서 확인)

| 문서 | 내용 | 언제 |
|---|---|---|
| `docs/01_contest.md` | 공모전 규정, 일정, 심사 기준, 개발 환경 요구사항 | 범위나 일정 판단 시 |
| `docs/02_strategy.md` | 입상 전략, 차별점, 심사 항목별 대응, PT 예상 질문 | 기능 우선순위 판단 시 |
| `docs/03_sdk_reference.md` | Pleos SDK 가용성, Vehicle SDK 신호 목록, 없는 신호 | **SDK 관련 코드 작성 전 필수** |
| `docs/04_usecases.md` | 유즈케이스 v2 (UC ID, 우선순위, 관측 단서 규칙) | 기능 구현 전 필수 |
| `docs/05_prototype_plan.md` | 아키텍처, 마일스톤, 시나리오 재생(SIL) 설계 | 구현 시작 전 |
| `docs/06_open_questions.md` | 미확인 사항, 해야 할 확인 작업 | 막혔을 때 |
| `docs/07_decision_log.md` | 지금까지의 결정과 이유 | 방향을 바꾸고 싶을 때 |
| `docs/08_dev_overview.md` | 소프트웨어 아키텍처 설명서 (계층, 의존 규칙, 설계 제약, 요구사항 구현 현황) | 전체 파악, 기획서 작성 시 |
| `docs/09_view_guide.md` | View 계층 개발 매뉴얼 (작업 범위, View 계약, 요구사항 VR-xx, 절차, AI 지시문) | **`ui/` 작업 전 필수** |
| `docs/reference/` | 원본 자료 (Notion Charter, Ideas, 외부 피드백, UC v1) | 배경이 필요할 때만 |

## 4. 확정된 제약 (바꾸려면 반드시 사용자에게 먼저 확인)

1. **외부 API 사용 금지.** 날씨, LLM, 외부 지도 타일, 자체 클라우드 서버 모두 사용하지 않는다. Pleos SDK, Android 표준 API, 앱 내부 로직만 쓴다.
2. **준비 중 SDK(STT, TTS, FusedLocation, Vehicle Control)도 설계에는 포함**한다. 단 실제로 호출할 수 없으면 데모는 더미 또는 시나리오 재생 신호로 시연한다.
3. **Vehicle SDK에는 차량 속도, 기어, 시동 상태 신호가 없다.** 이 신호를 가정한 코드를 쓰지 않는다.
4. **관측 사실과 해석을 분리한다.** "와이퍼 작동 = 비"처럼 단정하지 않는다. 흐름은 항상 `관측 사실 → 단서 제안 → 사용자 확인 → 저장`.
5. **주행 중 화면 입력 금지.** 입력, 편집, 회고 시작 UI는 CarUxRestrictions 기준 제한이 풀린 상태에서만 연다. 상태를 알 수 없으면 닫힌 쪽으로 동작한다(fail-safe).
6. **트립 종료 추정은 "안전한 주차 판정"이 아니다.** 트립 로직 결과를 안전 관련 판단이나 UX 제한 해제 근거로 쓰지 않는다.
7. **데이터는 로컬 저장.** 위치 이력, 음성 텍스트, 동승자 정보는 기기 안에만 둔다.
8. STT/TTS가 서버를 거칠 수 있으므로(HYBRID 모드, 확인 필요) 문서나 UI에 "완전 온디바이스"라고 쓰지 않는다. 표현은 "외부 API를 직접 사용하지 않음".

## 5. 기술 스택

- 언어: Kotlin
- minSdk 28 이상, Gradle 8.0 이상 (공모전 요구사항)
- UI: Jetpack Compose를 기본으로 하되, Pleos 공식 샘플이 다른 방식을 요구하면 샘플을 따른다
- 저장: Room
- 비동기: Kotlin Coroutines, Flow
- 테스트: JUnit (도메인 로직), 시나리오 재생 기반 통합 테스트

## 6. 아키텍처 원칙 (상세는 docs/05_prototype_plan.md)

```
[신호 소스]                     [도메인 로직, 소스 무관]          [UI]
VehicleSignalSource ──┐
  ├ PleosVehicleSource │        TripDetector (UC-01)
  ├ ReplaySource(CSV)  ├──Flow──► ClueExtractor (UC-03)  ──► Room ──► Timeline (UC-11)
  └ ManualDummySource  │        SceneSuggester (UC-12)                Scene 확인 (UC-12)
SpeechSource (Pleos/Fake) ┘     DrivingGate (UC-09)                  회고 입력 (UC-07b)
LocationSource (Pleos/Replay)
```

- **계층은 MVVM: `ui`(View) → `presentation`(ViewModel, View 계약) → `domain` / `data` / `signal` / `speech` / `platform`.** 상세와 규칙은 `docs/05_prototype_plan.md`의 "정적 구조".
- **View 작업은 `ui/` 안에서만 한다.** 화면은 `JournalUiState`를 그리고 `JournalActions`만 호출한다. `ui/`에서 data, signal, speech, platform, Room, SDK를 import 하지 않는다. View 외 작업에서 `ui/`의 디자인을 임의로 바꾸지 않는다.
- 의존 규칙은 `ArchitectureTest`가 검사한다. 이 테스트를 끄거나 금지 목록을 줄이려면 사용자에게 먼저 확인한다.
- 모든 외부 입력은 인터페이스 뒤에 둔다. 도메인 로직은 실제 SDK를 몰라야 한다.
- 소스 선택은 빌드 설정 또는 디버그 메뉴로 바꾼다.
- **화면에 현재 입력 출처(REAL / REPLAY / DUMMY) 배지를 표시**한다. PT에서 "실제로 되는 것과 더미는?" 질문에 화면으로 답하기 위함.

## 7. 코딩 규칙

- **Pleos SDK의 패키지명, 클래스명, 메서드명, 퍼미션 문자열, Gradle 의존성 좌표를 추측해서 쓰지 않는다.** 공식 문서(`docs/03_sdk_reference.md`의 URL)나 공식 샘플에서 확인한 것만 쓰고, 출처 URL을 주석으로 남긴다. 확인이 안 되면 TODO로 남기고 사용자에게 알린다.
- 기능 코드와 커밋 메시지에 UC ID를 참조한다. 예: `feat(UC-01): 트립 종료 후보 판정 추가`
- 도메인 로직(TripDetector, ClueExtractor 등)은 테스트를 먼저 쓴다. 시나리오 CSV로 재현 가능한 테스트를 선호한다.
- 커밋은 작게, 동작하는 단위로. 커밋과 push는 사용자가 요청할 때만.
- 새 의존성 추가 전 이유를 설명한다. 외부 네트워크가 필요한 라이브러리는 추가하지 않는다.

## 8. 사용자와의 대화 규칙

- 한국어로 대화. 업계 표준 용어(ADAS, SOTIF, SIL 등)는 영어 그대로.
- 결론 먼저, 짧게. 불확실한 것은 "확인 필요"로 명시하고 추측하지 않는다.
- 문장에 '·' 기호를 쓰지 않는다. 나열은 ','로.
- 듣기 좋은 말보다 입상과 품질에 필요한 바른 말을 한다.

## 9. 현재 상태 (2026-11-14 기준)

- M0~M3 구현 완료, MVVM 구조. P1 유즈케이스 8개가 에뮬레이터(Pleos Connect v2.1.0)에서 동작한다. 트립 분할은 정차 지점에서만 가능.
- M4 일부 완료: 실제 Vehicle SDK 신호 7종을 기록 흐름에 연결(`PleosVehicleSource`, 화면의 "실제 신호 기록"). 값 해석 일부는 에뮬레이터 관측에 근거하며 실차 미검증(`docs/06_open_questions.md` 17, 20번).
- 음성 인식은 Fake(시나리오 문장), 음성 합성은 소리 없음. Pleos STT/TTS는 문서상 준비 중.
- 남은 것: 데모 영상 녹화, release APK, P2/P3 유즈케이스, 백그라운드 기록.
- 역할: 아키텍처 담당은 View 외 전부, View 담당은 `ui/`, 기능 취사선택, 기획서.
- 1차 서류 접수 마감: **2026-11-15 23:59**. 목표는 데모 영상 또는 데모 APK까지 제출(가산점).
- 2차 오프라인 PT: **2026-12-03 ~ 12-04**. PT 전까지 실제 동작하는 앱 필수.
- 다음 작업: View 디자인과 기능 취사선택 → 데모 영상 녹화 → release APK.
- 코드 저장소: GitHub private `pleos-challenge-lab` (임시 이름, 변경 예정).
- 에뮬레이터 실행, 신호 변경 방법: Virtual Console(`adb forward tcp:8088 tcp:8088` 후 http://127.0.0.1:8088/).
