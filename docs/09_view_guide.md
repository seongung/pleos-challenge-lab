# 09. View 계층 개발 매뉴얼

| 항목 | 내용 |
|---|---|
| 문서 ID | PJ-VIEW-09 |
| 대상 | `ui` 패키지 (`app/src/main/java/com/pleosjournal/ui/`) |
| 기준일 | 2026-11-14 |
| 독자 | View 계층 개발자, View 작업을 수행하는 AI 도구 |
| 관련 문서 | `08_dev_overview.md` (아키텍처), `04_usecases.md` (요구사항) |

## 1. 목적과 범위

본 문서는 View 계층의 작업 범위, View 계약, 준수 요구사항, 작업 절차, 검증 방법을 정의한다.
화면 디자인 자체(색, 배치, 문구)는 규정하지 않으며 View 계층의 재량이다.

### 1.1 작업 범위

| 구분 | 대상 | 권한 |
|---|---|---|
| View 계층 | `ui/` 패키지 전체. 하위 패키지와 파일 추가 포함 | 변경 가능 |
| View 계약 | `presentation/JournalUiState.kt` | 읽기 전용. 변경은 요청 절차(5.3)를 따른다 |
| 그 외 | `presentation`, `domain`, `data`, `signal`, `speech`, `platform`, Gradle 설정, 매니페스트 | 변경 불가 |

### 1.2 현재 구성

| 파일 | 내용 |
|---|---|
| `ui/JournalScreen.kt` | 화면 전체. 타임라인, 트립 카드, 장면 후보, 회고, 프라이버시 설정 |
| `ui/SampleState.kt` | 미리보기용 표본 상태(`sampleState`)와 무동작 구현(`NoActions`) |

## 2. View 계약

### 2.1 진입점

```kotlin
@Composable
fun JournalScreen(state: JournalUiState, actions: JournalActions)
```

진입점의 이름과 매개변수는 고정이다. 내부 구현은 제한 없이 변경할 수 있다.
View는 `state`로 화면을 구성하고, 사용자의 동작을 `actions`의 함수 호출로 전달한다. 그 외 경로로 데이터를 읽거나 쓰지 않는다.

계약의 기준 정의는 `presentation/JournalUiState.kt`이며, 본 절과 다를 경우 코드가 우선한다.

### 2.2 상태: `JournalUiState`

| 필드 | 형식 | 의미 |
|---|---|---|
| `inputSource` | String | 차량 신호의 입력 출처. REAL, REPLAY, DUMMY |
| `sttSource` | String | 음성 인식의 입력 출처 |
| `inputAllowed` | Boolean | 입력 허용 여부. 주행 제한 중이거나 상태를 알 수 없으면 false |
| `voiceAllowed` | Boolean | 음성 입력 허용 여부. 입력 허용이면서 음성 기록에 동의한 경우 true |
| `status` | String | 기록기 상태 문자열. 디버그와 데모 용도 |
| `scenarios` | List | 재생 가능한 시나리오(`file`, `title`). 디버그와 데모 용도 |
| `trips` | List | 트립 목록. 최신순 |
| `privacy` | `PrivacyUi` | 프라이버시 설정 상태 |

`TripUi`

| 필드 | 형식 | 의미 |
|---|---|---|
| `id` | Long | 트립 식별자 |
| `startMs`, `endMs` | Long | 시작, 종료 시각 (epoch ms) |
| `distanceKm` | Double | 이동 거리 |
| `source` | String | 이 트립의 입력 출처 |
| `spans` | List | 관측 구간 |
| `scenes` | List | 장면 후보. 사용자가 거부한 후보는 포함되지 않는다 |
| `memoQuestion` | String | 트립 회고 질문 |
| `memo`, `memoSource` | String? | 회고 답변과 그 입력 출처. 없으면 null |
| `memoListening` | Boolean | 회고 답변을 듣는 중인지 여부 |
| `canMergeWithPrevious` | Boolean | 직전 트립과 병합 가능 여부 |
| `mergeCandidateGapMin` | Long? | 병합 후보인 경우 직전 트립과의 간격(분). 후보가 아니면 null |

`SpanUi`

| 필드 | 형식 | 의미 |
|---|---|---|
| `kind` | `ClueKind` | 관측 종류. WIPER, HEADLAMP, WINDOW_OPEN, REAR_RECLINE, DOOR_OPEN |
| `target` | String | 대상 위치. DRIVER, FRONT_PASSENGER, FRONT_LEFT, FRONT_RIGHT, REAR_LEFT, REAR_RIGHT, FRONT, 또는 빈 문자열 |
| `startMs`, `endMs` | Long | 구간의 시작, 종료 시각 |
| `startFraction`, `endFraction` | Float | 트립 전체를 0~1로 둔 상대 위치. 시간축 표시용 |

`SceneUi`

| 필드 | 형식 | 의미 |
|---|---|---|
| `id` | Long | 장면 후보 식별자 |
| `kind` | `ClueKind` | 근거가 된 관측 종류 |
| `question` | String | 질문 문구 |
| `isOpenQuestion` | Boolean | true면 음성으로 답하는 질문, false면 확인 또는 거부로 답하는 질문 |
| `evidence` | List | 근거 관측 구간 |
| `confirmed` | Boolean | 사용자 확인 여부 |
| `answer`, `answerSource` | String? | 음성 답변과 그 입력 출처 |
| `listening` | Boolean | 이 질문의 답변을 듣는 중인지 여부 |

`PrivacyUi`

| 필드 | 형식 | 의미 |
|---|---|---|
| `paused` | Boolean | 기록 일시정지 여부 |
| `voiceConsent` | Boolean | 음성 기록 동의 여부 |
| `kinds` | List | 관측 종류별 수집 여부 (`kind`, `enabled`) |

### 2.3 동작: `JournalActions`

| 동작 | 효과 | 선행 조건 | 비가역 |
|---|---|---|---|
| `replay(file)` | 시나리오를 재생한다. 디버그와 데모 용도 | 없음 | |
| `confirmScene(sceneId)` | 장면 후보를 확인 처리한다 | `inputAllowed` | |
| `denyScene(sceneId)` | 장면 후보를 숨긴다. 관측 구간은 유지된다 | `inputAllowed` | |
| `answerSceneByVoice(sceneId)` | 질문을 음성으로 듣고 답변을 저장한다 | `voiceAllowed` | |
| `recordMemoByVoice(tripId)` | 트립 회고를 음성으로 듣고 저장한다 | `voiceAllowed` | |
| `mergeWithPrevious(tripId)` | 직전 트립과 병합한다 | `inputAllowed`, `canMergeWithPrevious` | 예 |
| `deleteTrip(tripId)` | 트립과 종속 데이터를 삭제한다 | `inputAllowed` | 예 |
| `setPaused(paused)` | 기록 일시정지를 설정한다 | `inputAllowed` | |
| `setVoiceConsent(consent)` | 음성 기록 동의를 설정한다 | `inputAllowed` | |
| `setKindEnabled(kind, enabled)` | 관측 종류별 수집을 설정한다 | `inputAllowed` | |
| `deleteAll()` | 모든 기록을 삭제한다 | `inputAllowed` | 예 |

선행 조건을 충족하지 않은 호출은 ViewModel이 무시한다. 오류는 발생하지 않는다.
동작의 결과는 반환값이 아니라 갱신된 `state`로 전달된다.

## 3. 요구사항

"~해야 한다"는 필수, "~할 수 있다"는 허용이다.

### 3.1 구조 요구사항

`ArchitectureTest`가 자동으로 검증한다.

| ID | 요구사항 |
|---|---|
| VR-01 | `ui`는 `com.pleosjournal.data`, `signal`, `speech`, `platform`을 import 하지 않아야 한다 |
| VR-02 | `ui`는 `androidx.room`, `androidx.lifecycle`, `android.car`, `ai.pleos`를 import 하지 않아야 한다 |
| VR-03 | `ui`가 참조하는 프로젝트 코드는 `com.pleosjournal.presentation`과 `com.pleosjournal.domain`의 모델로 한정해야 한다 |

### 3.2 표시 요구사항

자동 검증 대상이 아니다. 리뷰로 확인한다.

| ID | 요구사항 | 근거 |
|---|---|---|
| VR-10 | 관측 구간은 사실로만 표시해야 한다. 문구, 아이콘, 색으로 해석을 부여하지 않아야 한다 | DC-02 |
| VR-11 | 장면 후보의 질문은 근거 관측 구간(`evidence`)과 함께 표시해야 한다 | DC-02 |
| VR-12 | 확인되지 않은 장면 후보를 확정된 사실처럼 표시하지 않아야 한다 | DC-02 |
| VR-13 | 입력 출처(`inputSource`, `sttSource`, 트립의 `source`, 답변의 출처)를 화면에 표시해야 한다. 형태와 위치는 변경할 수 있다 | DC-08 |
| VR-14 | "완전 온디바이스"라는 표현을 사용하지 않아야 한다. "외부 API를 직접 사용하지 않음", "기기 안에만 저장"은 사용할 수 있다 | DC-06 |

### 3.3 상호작용 요구사항

| ID | 요구사항 | 근거 |
|---|---|---|
| VR-20 | `inputAllowed`가 false이면 입력, 편집, 회고, 설정 변경 UI를 제공하지 않아야 한다 | DC-03 |
| VR-21 | `voiceAllowed`가 false이면 음성 입력 UI를 제공하지 않아야 한다 | DC-03, UC-18 |
| VR-22 | 비가역 동작(`deleteTrip`, `deleteAll`)은 사용자의 재확인을 거쳐 호출해야 한다 | 오조작 방지 |
| VR-23 | 문자 입력(키보드)을 요구하지 않아야 한다. 입력 수단은 버튼과 음성으로 한정한다 | 차량 환경 |
| VR-24 | 듣는 중(`listening`, `memoListening`)에는 다른 음성 입력을 시작할 수 없게 해야 한다 | 중복 입력 방지 |

### 3.4 자원 요구사항

| ID | 요구사항 | 근거 |
|---|---|---|
| VR-30 | 네트워크로 불러오는 이미지, 글꼴, 지도 타일을 사용하지 않아야 한다 | DC-01 |
| VR-31 | 라이브러리 의존성을 추가하지 않아야 한다. 필요한 경우 요청 절차(5.3)를 따른다 | 작업 범위 |

## 4. 실행 환경

| 항목 | 내용 |
|---|---|
| 표시 영역 | 가로로 긴 화면. 에뮬레이터 전체 2560x1440 중 오른쪽 약 2/3. 왼쪽은 플랫폼의 차량 상태 영역 |
| 배율 | 전체 UI를 1.5배로 확대한 상태 (`JournalScreen` 상단의 `Density` 설정). 조정할 수 있다 |
| 테마 | 어두운 테마 |
| 주행 중 | 플랫폼이 앱 화면 전체를 가린다 (PF-01). 주행 중 화면은 설계 대상이 아니다. `inputAllowed == false` 상태의 화면은 상태 미확인 시의 안전장치로만 유지한다 |

## 5. 작업 절차

### 5.1 작업 유형별 변경 위치

| 작업 | 변경 위치 |
|---|---|
| 디자인 변경 (색, 글꼴, 배치, 구성요소 형태) | `ui/` |
| 버튼, 라벨, 안내 문구 변경 | `ui/JournalScreen.kt` |
| 기능의 화면 제거 | `ui/`에서 해당 부분을 표시하지 않는다. 다른 계층은 변경하지 않는다 |
| 화면 전환, 탭, 펼침, 재확인 등 표시 방식에 한정된 상태 | `ui/` 내부에서 `remember`로 관리한다 |
| 미리보기 상태 추가 | `ui/SampleState.kt` |
| 장면 질문 문구 변경 | `domain/SceneSuggester.kt`. 요청 절차(5.3) |
| 계약에 없는 데이터 또는 동작 | 요청 절차(5.3) |

### 5.2 검증

| 단계 | 방법 | 통과 기준 |
|---|---|---|
| 미리보기 | Android Studio에서 `JournalScreen.kt`의 Split 또는 Design 보기 | 대상 상태가 의도대로 표시된다 |
| 단위 시험 | `./gradlew testDebugUnitTest` | 전체 통과. `ArchitectureTest` 포함 |
| 에뮬레이터 | `./gradlew installDebug` 후 시나리오 재생 | 3장의 요구사항을 충족한다 |

에뮬레이터 확인 시 참고 사항:

- "재생: 제천 당일치기"는 약 16초 후 트립 1건과 장면 후보 4건을 생성한다.
- "재생: 편의점 경유"는 트립 2건과 병합 후보 표시를 생성한다.
- 음성 입력 UI는 프라이버시 설정에서 음성 기록 동의를 켠 경우에만 나타난다.

### 5.3 계약 변경 요청

View 계약에 없는 데이터나 동작이 필요한 경우 다음을 정리해 아키텍처 담당에게 요청한다.

1. 필요한 필드 또는 동작의 이름과 의미
2. 사용하려는 화면과 목적
3. 관련 유즈케이스 ID

계약은 추가 방식으로만 변경한다. 기존 필드의 의미는 바꾸지 않는다. 계약이 갱신되기 전에는 View에서 임시 값이나 임의 구현으로 대체하지 않는다.

## 6. 알려진 결함

현재 View 구현의 결함이며 개선 대상이다.

| ID | 내용 |
|---|---|
| VD-01 | 타임라인에 트립이 있는 상태에서 새 트립이 추가되면 스크롤 위치가 유지되어 새 트립이 화면 밖에 놓인다 |
| VD-02 | 관측 구간이 많은 트립 카드는 한 화면을 넘어, 스크롤해야 회고 입력이 보인다 |
| VD-03 | 디버그 요소(시나리오 재생, 상태 문자열)가 본 화면에 함께 표시된다. 데모에 필요하므로 접근 경로는 유지해야 한다 |
| VD-04 | 프라이버시 설정이 독립된 화면이 아니라 같은 화면 안에서 전환된다 |
| VD-05 | 확인 또는 거부한 장면 후보를 되돌리는 UI가 없다. 대응하는 동작도 계약에 없다 |
| VD-06 | 듣는 중에 해당 버튼만 비활성화되고 다른 음성 버튼은 활성 상태로 남는다 (VR-24 미충족). 중복 호출은 ViewModel이 무시하므로 동작 오류는 없다 |

## 부록 A. AI 도구용 지시문

View 작업을 AI 도구에 지시할 때 대화의 첫머리에 사용한다.
이 저장소에서 Claude Code를 사용하는 경우 `CLAUDE.md`가 자동으로 적용되므로 마지막 줄만 작성해도 된다.

```
이 저장소는 차량용 Android 앱(Kotlin, Jetpack Compose)이다. 이번 작업의 대상은 View 계층이다.

먼저 읽을 것:
- docs/09_view_guide.md (View 계층 개발 매뉴얼)
- app/src/main/java/com/pleosjournal/presentation/JournalUiState.kt (View 계약)
- app/src/main/java/com/pleosjournal/ui/JournalScreen.kt (현재 화면)

준수 사항:
- app/src/main/java/com/pleosjournal/ui/ 안의 파일만 변경한다. 그 외 코드, Gradle 설정, 매니페스트는 변경하지 않는다.
- JournalScreen(state: JournalUiState, actions: JournalActions) 의 이름과 매개변수는 변경하지 않는다.
- 화면은 state만으로 구성하고, 동작은 actions의 함수로만 요청한다. DB, SDK, 네트워크에 직접 접근하지 않는다.
- docs/09_view_guide.md 3장의 요구사항(VR-01 ~ VR-31)을 준수한다.
- 계약에 없는 데이터나 동작이 필요하면 코드를 임의로 만들지 말고, 필요한 필드와 동작을 목록으로 정리해 보고한다.
- 변경한 화면 상태마다 ui/SampleState.kt의 표본 상태로 @Preview를 작성한다.
- 작업 후 ./gradlew testDebugUnitTest 를 실행하고 결과를 보고한다.

이번 작업: (작업 내용을 기술한다)
```

작업 기술 예:

- 트립 카드를 접고 펼 수 있게 한다. 접힌 상태에는 날짜, 시간, 거리, 미확인 질문 수만 표시한다.
- 장면 후보를 트립 카드 내부가 아니라 한 건씩 넘기는 별도 화면으로 제시한다.
- 관측 구간 막대를 종류별 색으로 구분한다. 색에 해석을 담지 않는다 (VR-10).
