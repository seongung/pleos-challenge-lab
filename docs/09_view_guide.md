# 09. 화면(View) 작업 가이드

화면 담당(빅터)과, 화면 작업을 돕는 AI가 읽는 문서. 2026-11-14 기준.
AI에게 일을 시킬 때는 맨 아래 "AI에게 줄 지시문"을 그대로 붙여 넣으면 된다.

## 1. 작업 범위

고쳐도 되는 곳은 `app/src/main/java/com/pleosjournal/ui/` 하나다.

| 파일 | 내용 |
|---|---|
| `ui/JournalScreen.kt` | 화면 전체. 타임라인, 트립 카드, 장면 후보, 회고, 프라이버시 설정 |
| `ui/SampleState.kt` | 미리보기용 가짜 상태와 아무 일도 안 하는 동작(`NoActions`) |

파일을 나누거나 새 파일을 만드는 것은 자유다. `ui/` 안이면 된다(예: `ui/theme/`, `ui/components/`).

읽기만 하는 곳: `presentation/JournalUiState.kt` (화면이 받는 상태와 부를 수 있는 동작의 전체 목록).

그 밖(`presentation`, `domain`, `data`, `signal`, `speech`, `platform`, Gradle 설정, 매니페스트)은 고치지 않는다. 필요하면 상주에게 요청한다.

## 2. 화면이 받는 것과 부를 수 있는 것

```kotlin
@Composable
fun JournalScreen(state: JournalUiState, actions: JournalActions)
```

이 함수의 이름과 매개변수는 바꾸지 않는다(`MainActivity`가 부른다). 안쪽은 전부 바꿔도 된다.

### 상태 (`JournalUiState`)

| 필드 | 의미 |
|---|---|
| `inputSource`, `sttSource` | 입력 출처 배지에 쓸 글자 (REAL / REPLAY / DUMMY) |
| `inputAllowed` | false면 입력, 편집, 회고 UI를 열지 않는다 |
| `voiceAllowed` | 음성 입력 가능 여부 (주행 제한 없음 + 음성 기록 동의) |
| `status` | 기록기 상태 한 줄 (디버그, 데모용) |
| `scenarios` | 재생 가능한 시나리오 목록 (디버그, 데모용) |
| `trips` | 최신순 트립 목록 |
| `privacy` | 일시정지, 음성 동의, 항목별 수집 상태 |

트립(`TripUi`): 시작, 끝 시각(ms), 거리, 출처, 관측 구간 `spans`, 장면 후보 `scenes`, 회고 질문과 답(`memoQuestion`, `memo`), 병합 가능 여부와 후보 간격.

관측 구간(`SpanUi`): 종류(`kind`), 대상 위치(`target`), 시작, 끝 시각, 트립 안에서의 위치 비율(`startFraction`, `endFraction`. 0~1, 시간축 막대용).

장면 후보(`SceneUi`): 질문(`question`), 말로 답하는 질문인지(`isOpenQuestion`), 근거 구간(`evidence`), 확인 여부, 답변, 듣는 중 여부.

### 동작 (`JournalActions`)

`replay(file)`, `confirmScene(id)`, `denyScene(id)`, `answerSceneByVoice(id)`, `recordMemoByVoice(tripId)`,
`mergeWithPrevious(tripId)`, `deleteTrip(tripId)`, `setPaused(b)`, `setVoiceConsent(b)`, `setKindEnabled(kind, b)`, `deleteAll()`

정확한 정의와 설명은 `presentation/JournalUiState.kt`의 주석이 기준이다.

## 3. 자주 하는 작업

| 하고 싶은 것 | 방법 |
|---|---|
| 디자인(색, 글꼴, 배치, 카드 모양) 변경 | `ui/`만 고친다 |
| 버튼, 라벨, 안내 문구 변경 | `ui/JournalScreen.kt`의 문자열을 고친다 |
| 화면에서 기능 빼기 | 그 부분을 그리지 않는다. 다른 코드는 그대로 둔다 |
| 화면 전환, 탭, 펼침, 삭제 재확인 같은 "보이는 방식" | `ui/` 안에서 `remember`로 관리한다 |
| 새 화면 상태 미리 보기 | `ui/SampleState.kt`에 가짜 상태를 추가하고 `@Preview`를 만든다 |
| 장면 질문 문구("이때 비가 왔나요?") 변경 | `domain/SceneSuggester.kt`에 있다. 상주에게 요청 |
| 지금 상태에 없는 데이터나 새 동작 필요 | 상주에게 요청. `JournalUiState.kt`에 추가된 뒤 화면에서 쓴다 |

## 4. 지켜야 할 규칙

구조 규칙 (어기면 `ArchitectureTest`가 실패한다):

- `ui/`에서 `com.pleosjournal.data`, `signal`, `speech`, `platform`, `androidx.room`, `androidx.lifecycle`, `android.car`, `ai.pleos`를 import 하지 않는다.
- `ui/`에서 쓸 수 있는 우리 코드는 `com.pleosjournal.presentation.*`과 `com.pleosjournal.domain.ClueKind` 같은 모델뿐이다.

제품 규칙 (테스트가 못 잡는다. 사람이 지킨다):

1. **단정하지 않는다.** 관측 구간 라벨은 사실만 쓴다("와이퍼 작동 14:02~14:40"). "비 오는 날" 같은 해석을 라벨, 아이콘, 색으로 붙이지 않는다. 해석은 질문으로만 나오고, 사용자가 확인한 것만 확정으로 보여준다.
2. **근거를 숨기지 않는다.** 장면 질문 옆에는 `evidence`(근거 구간)를 항상 함께 보여준다.
3. **출처 배지를 없애지 않는다.** `inputSource`, `sttSource`, 트립의 `source`는 화면에 보여야 한다. 심사에서 "실제와 더미의 구분"을 화면으로 답하기 위함이다. 모양과 위치는 바꿔도 된다.
4. **`inputAllowed`가 false면 입력 UI를 열지 않는다.** 확인, 거부, 음성, 병합, 삭제, 설정 변경 모두 해당한다. `voiceAllowed`가 false면 음성 버튼을 열지 않는다.
5. **되돌릴 수 없는 동작은 재확인한다.** `deleteTrip`, `deleteAll`은 한 번 누름으로 실행되지 않게 한다.
6. **키보드 입력을 넣지 않는다.** 차량 화면이다. 입력은 버튼과 음성으로 한다.
7. **"완전 온디바이스"라고 쓰지 않는다.** 표현은 "외부 API를 직접 사용하지 않음". "기기 안에만 저장"은 써도 된다.
8. **외부 리소스를 쓰지 않는다.** 네트워크로 불러오는 이미지, 글꼴, 지도 타일 금지. 아이콘과 글꼴은 앱에 포함된 것만 쓴다.
9. **새 라이브러리를 추가하지 않는다.** 필요하면 상주에게 요청한다(Gradle 파일은 화면 작업 범위 밖).

## 5. 화면 환경

- 에뮬레이터의 앱 영역은 가로로 긴 화면이다(전체 2560x1440 중 오른쪽 약 2/3). 왼쪽은 차량 상태 영역이다.
- 멀리서 보는 화면이라 현재 전체 UI를 1.5배로 키워 두었다(`JournalScreen` 맨 위의 `Density` 설정). 조정해도 된다.
- 어두운 테마가 기본이다.
- **주행 중(기어 D)에는 플랫폼이 앱 화면 전체를 가린다.** 주행 중 화면은 디자인할 필요가 없다. `inputAllowed == false`인 화면은 "상태를 아직 모를 때"의 안전장치로만 남긴다.

## 6. 확인 방법

1. **미리보기**: Android Studio에서 `JournalScreen.kt`를 열고 오른쪽 위 Split 또는 Design. 에뮬레이터가 필요 없다.
2. **테스트**: `./gradlew testDebugUnitTest` (계층 규칙 위반 여부 포함).
3. **에뮬레이터**: `./gradlew installDebug` 후 앱에서 "재생: 제천 당일치기"를 누른다. 16초쯤 뒤 트립과 장면 후보가 뜬다.
   병합 화면은 "재생: 편의점 경유"로 본다. 음성 버튼은 프라이버시 설정에서 "음성 기록 동의"를 켜야 보인다.

## 7. 지금 화면의 알려진 문제 (개선 대상)

- 타임라인에 트립이 이미 있을 때 새 트립이 위에 추가되면 스크롤이 기존 위치에 머물러 새 트립이 안 보인다.
- 제천 트립 카드는 한 화면을 넘어, 스크롤해야 회고 버튼이 보인다.
- 디버그용 요소(시나리오 재생 버튼, 상태 한 줄)가 본 화면에 섞여 있다. 데모 영상에서는 재생 버튼이 필요하니, 숨기더라도 접근할 방법은 남긴다.
- 프라이버시 설정이 별도 화면이 아니라 같은 화면 안에서 바뀐다.
- 확인하거나 거부한 장면 후보를 되돌리는 UI가 없다(동작도 아직 없다. 필요하면 요청).

## 8. AI에게 줄 지시문

아래를 AI 대화 맨 앞에 붙여 넣는다. 이 저장소에서 Claude Code를 쓰면 `CLAUDE.md`가 자동으로 읽히므로 "이번 작업" 줄만 써도 된다.

```
이 저장소는 차량용 Android 앱(Kotlin, Jetpack Compose)이다. 나는 화면(View) 담당이다.

먼저 읽을 것:
- docs/09_view_guide.md (이 작업의 규칙)
- app/src/main/java/com/pleosjournal/presentation/JournalUiState.kt (화면이 받는 상태와 부를 수 있는 동작)
- app/src/main/java/com/pleosjournal/ui/JournalScreen.kt (현재 화면)

규칙:
- app/src/main/java/com/pleosjournal/ui/ 안의 파일만 고친다. 그 밖의 코드, Gradle 설정, 매니페스트는 고치지 않는다.
- JournalScreen(state: JournalUiState, actions: JournalActions) 의 이름과 매개변수는 바꾸지 않는다.
- 화면은 state만 보고 그리고, 동작은 actions의 함수만 부른다. DB, SDK, 네트워크를 직접 쓰지 않는다.
- 새 라이브러리, 외부 이미지, 외부 글꼴을 추가하지 않는다.
- 상태에 없는 데이터나 새 동작이 필요하면 코드를 지어내지 말고, 필요한 필드와 동작을 목록으로 정리해 나에게 알려준다.
- docs/09_view_guide.md 4번의 제품 규칙(단정 금지, 근거 표시, 출처 배지 유지, inputAllowed 준수, 삭제 재확인)을 지킨다.
- 바꾼 화면 상태마다 ui/SampleState.kt의 가짜 상태로 @Preview를 만든다.
- 끝나면 ./gradlew testDebugUnitTest 를 실행해 통과를 확인하고 결과를 알려준다.

이번 작업: (여기에 하고 싶은 것을 쓴다)
```

요청 예:

- "트립 카드를 접었다 펼 수 있게 해줘. 접힌 상태에는 날짜, 시간, 거리, 확인 안 한 질문 수만 보이게."
- "장면 후보를 카드 안이 아니라 트립 종료 직후에 뜨는 한 장씩 넘기는 화면으로 바꿔줘."
- "관측 구간 막대를 종류별 색으로 구분해줘. 색에 해석(비, 밤)을 담지는 마."
