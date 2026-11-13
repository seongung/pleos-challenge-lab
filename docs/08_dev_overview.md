# 08. 소프트웨어 아키텍처 설명서

| 항목 | 내용 |
|---|---|
| 문서 ID | PJ-SWAD-08 |
| 대상 | Pleos Journal (가제) Android 앱 |
| 기준일 | 2026-11-14 |
| 기준 구현 | 마일스톤 M0~M3, M4 일부(실제 차량 신호 소스), 단위 테스트 25건 |
| 관련 문서 | `04_usecases.md` (요구사항 원문), `05_prototype_plan.md` (마일스톤), `06_open_questions.md` (미확인 사항), `09_view_guide.md` (View 계층 매뉴얼) |

## 1. 목적과 범위

본 문서는 Pleos Journal 앱의 소프트웨어 구조, 계층 간 인터페이스, 설계 제약, 요구사항별 구현 현황을 정의한다.
코드를 읽지 않고 시스템의 구성과 현재 상태를 파악하는 데 쓴다. 상세 알고리즘과 화면 디자인은 범위에 포함하지 않는다.

## 2. 시스템 개요

Pleos Journal은 Pleos Connect(Android Automotive 기반) 위에서 동작하는 차량용 드라이브 저널 앱이다.
이동 중 관측한 차량 상태를 구간으로 기록하고, 트립 종료 후 그 관측을 근거로 질문을 제시하며, 사용자가 확인하거나 음성으로 답한 내용만 저널에 남긴다.

처리 원칙은 다음 한 가지다.

> 관측 사실 → 단서 제안(질문) → 사용자 확인 → 저장

시스템은 관측에서 해석을 단정하지 않는다. 예를 들어 와이퍼 작동을 "비"로 기록하지 않는다.

### 2.1 용어

| 용어 | 정의 |
|---|---|
| 트립 | 한 번의 이동. 시작과 종료는 차량 신호로 추정한다 |
| 관측 구간 | 특정 차량 상태가 지속된 시간 구간. 해석을 포함하지 않는다 |
| 장면 후보 | 관측 구간을 근거로 생성한 질문. 사용자 확인 전에는 사실로 취급하지 않는다 |
| 입력 출처 | 데이터가 들어온 경로. REAL(실제 SDK), REPLAY(시나리오 재생), DUMMY(가짜 구현) |
| 주행 제한 | 플랫폼의 CarUxRestrictions가 요구하는 주의분산 제한 상태 |
| View 계약 | View 계층과 나머지 계층 사이의 유일한 인터페이스 (`JournalUiState`, `JournalActions`) |

## 3. 아키텍처

### 3.1 구조 양식

MVVM, 단방향 데이터 흐름을 따른다. 상태는 ViewModel에서 View로만 내려가고, 사용자의 동작은 View에서 ViewModel로만 올라간다.

설계 목표: 기능의 추가와 제거, 화면 디자인 변경이 다른 계층의 수정 없이 가능해야 한다.

### 3.2 계층 구성

```
ui  ──►  presentation  ──►  domain / data / signal / speech / platform
```

| 계층 (패키지) | 책임 | 주요 구성요소 |
|---|---|---|
| `ui` | 화면 표시와 화면 내 상호작용 | `JournalScreen`, `SampleState` |
| `presentation` | 상태 조립, 동작 처리, 접근 제어, 입력 출처 선택 | `JournalUiState`, `JournalActions`, `JournalViewModel`, `Recorder` |
| `domain` | 판정 규칙. 순수 Kotlin | `TripDetector`, `merge`, `split`, `ClueExtractor`, `SceneSuggester`, `isInputAllowed` |
| `data` | 로컬 영속화 | `JournalDb` (Room), `PrivacyStore` |
| `signal` | 차량 신호 입력의 추상화 | `VehicleEvent`, `VehicleSignalSource`, `ReplaySource`, `PleosVehicleSource` |
| `speech` | 음성 입출력의 추상화 | `SpeechToText`, `TextToSpeech`, `FakeStt`, `FakeTts` |
| `platform` | 플랫폼 서비스 연동 | `DrivingGate` (CarUxRestrictions) |

### 3.3 의존 규칙

| ID | 규칙 | 검증 |
|---|---|---|
| AR-01 | `ui`는 `presentation`과 `domain`의 모델만 참조한다. `data`, `signal`, `speech`, `platform`, Room, SDK를 참조하지 않는다 | `ArchitectureTest` |
| AR-02 | `domain`은 Android, Room, SDK, `ui`, `presentation`을 참조하지 않는다 | `ArchitectureTest` |
| AR-03 | SDK 타입(`ai.pleos`, `android.car`)은 `signal`, `speech`, `platform` 밖으로 노출하지 않는다 | `ArchitectureTest` |
| AR-04 | 모든 외부 입력은 인터페이스 뒤에 둔다. 구현 선택은 `JournalViewModel` 한 곳에서 한다 | 코드 리뷰 |
| AR-05 | 접근 제어(주행 제한, 음성 동의)는 `presentation`에서 수행한다. View의 표시 여부에 의존하지 않는다 | 코드 리뷰 |

### 3.4 데이터 흐름

```
신호 소스 (REAL / REPLAY / DUMMY)
   │  VehicleEvent
   ▼
Recorder ─► TripDetector    트립 시작, 일시정지, 종료 추정
         ─► ClueExtractor   관측 구간 생성
         ─► SceneSuggester  장면 후보 생성
   │
   ▼
JournalDb (기기 로컬) ─► JournalViewModel ─► JournalUiState ─► ui
                                ▲                              │
                                └────── JournalActions ◄───────┘
```

모든 `VehicleEvent`는 발생 시각과 입력 출처를 포함한다. 도메인 로직은 이벤트 시각만 사용하며 시스템 시계를 읽지 않는다. 따라서 시나리오 재생 결과는 결정적이다.

실제 신호(REAL)는 이벤트가 없는 동안에도 트립 종료를 판정해야 하므로, `Recorder`가 30초 주기로 `TripDetector.tick`에 현재 시각을 전달한다. 재생(REPLAY)에서는 사용하지 않는다.

### 3.4.1 실제 신호 매핑 (`PleosVehicleSource`)

| 내부 신호 | SDK API | 변환 | 근거 |
|---|---|---|---|
| `SEAT_OCCUPIED` | `Seat.registerSeatOccupancy` | 원시값 false를 착석으로 본다 | 에뮬레이터 관측 (PF-04) |
| `DOOR_OPEN` | `Door.registerDoorPosition` | 위치값이 0보다 크면 열림 | 에뮬레이터 관측 |
| `TRIP_DISTANCE_KM` | `Odometer.registerDistanceDriven` | 누적 주행거리(km)를 그대로 사용. 판정은 증가분만 본다 | 공식 문서 (단위 km) |
| `WINDOW_POSITION_PCT` | `Window.registerWindowPosition` | 위치값을 그대로 사용. 0보다 크면 열림 | 에뮬레이터 관측 |
| `WIPER_ACTIVE` | `Wiper.registerWiperState` | 앞 유리의 `WiperState.ON` | 열거값 |
| `HEADLAMP_LOW` | `Light.registerLowBeamLightState` | `LightState.ON`. 주간주행등은 제외 | 열거값 |
| `SEAT_RECLINE_DEG` | `Seat.registerSeatBackrestAnglePosition` | 뒷좌석의 위치값을 그대로 사용 | 단위 미확인 |

좌석과 도어의 위치 대응은 좌핸들(LHD) 기준이다. 값의 의미가 공식 문서에 없는 항목은 `PleosVehicleSource`의 상수와 매핑 함수 한 곳에서 관리한다.

### 3.5 인터페이스

| 인터페이스 | 제공 | 사용 | 내용 |
|---|---|---|---|
| View 계약 | `presentation` | `ui` | `JournalUiState`(화면 상태), `JournalActions`(동작). 상세는 `09_view_guide.md` |
| `VehicleSignalSource` | `signal` | `presentation` | `events(): Flow<VehicleEvent>`, `source` |
| `SpeechToText` | `speech` | `presentation` | `listen(): String?`, `source` |
| `TextToSpeech` | `speech` | `presentation` | `speak(text)`, `source` |
| `JournalDao` | `data` | `presentation` | 트립, 관측 구간, 장면 후보의 조회와 변경 |
| 시나리오 CSV | `scenarios/` | `ReplaySource` | `t_ms,signal,target,value`. 형식은 `05_prototype_plan.md` |

## 4. 설계 제약

| ID | 제약 | 근거 |
|---|---|---|
| DC-01 | 외부 API를 사용하지 않는다 (날씨, LLM, 외부 지도 타일, 자체 서버) | 팀 결정 |
| DC-02 | 관측 사실과 해석을 분리한다. 해석은 질문으로만 제시한다 | 오인식 가능성 (와셔액, 터널, 오토라이트 등) |
| DC-03 | 입력, 편집, 회고 UI는 주행 제한이 없을 때만 연다. 상태를 알 수 없으면 닫는다 | 운전자 주의분산 |
| DC-04 | 트립 판정 결과를 안전 판단이나 주행 제한 해제 근거로 쓰지 않는다 | 트립 판정은 추정값 |
| DC-05 | 데이터는 기기 로컬에만 저장한다 | 프라이버시 |
| DC-06 | 문서와 UI에 "완전 온디바이스"라고 표기하지 않는다. 표현은 "외부 API를 직접 사용하지 않음" | STT/TTS의 서버 경유 가능성 미확인 |
| DC-07 | Pleos SDK의 식별자는 공식 문서나 공식 샘플에서 확인한 것만 사용한다 | 추측 구현 방지 |
| DC-08 | 화면에 입력 출처(REAL / REPLAY / DUMMY)를 표시한다 | 실제 동작과 모의 동작의 구분 |
| DC-09 | minSdk 28 이상, Gradle 8.0 이상, Kotlin | 공모전 요구사항 |

## 5. 요구사항 구현 현황

요구사항 원문은 `04_usecases.md`를 따른다.

| ID | 요구사항 | 구현 구성요소 | 검증 | 입력 출처 | 상태 |
|---|---|---|---|---|---|
| UC-01 | 트립 자동 초안 | `TripDetector`, `Recorder`, `PleosVehicleSource` | `TripDetectorTest` 8건, 에뮬레이터 확인 | REPLAY, REAL | 구현 |
| UC-03 | 관측 단서 기록 | `ClueExtractor`, `PleosVehicleSource` | `ClueExtractorTest` 5건, 에뮬레이터 확인 | REPLAY, REAL | 구현 |
| UC-11 | 저널 타임라인 | `JournalScreen` | 에뮬레이터 확인 | 저장 데이터 | 구현 (지도 제외) |
| UC-12 | 장면 후보 제안, 확인, 거부, 되돌리기 | `SceneSuggester`, `JournalViewModel` | `SceneSuggesterTest` 3건, 에뮬레이터 확인 | 저장 데이터 | 구현 |
| UC-07b | 음성 회고 | `SpeechToText`, `JournalViewModel` | 에뮬레이터 확인 | REPLAY (Fake STT) | 부분 구현 |
| UC-09 | 주행 제한 게이트 | `DrivingGate`, `isInputAllowed` | 판정 규칙 테스트 1건, 에뮬레이터 확인 | REAL | 구현 |
| UC-10 | 트립 보정 (병합, 분할, 삭제) | `merge`, `split`, `JournalDao` | `TripCorrectionTest` 4건, 에뮬레이터 확인 | 앱 내부 | 구현 (분할은 정차 지점에서만 가능) |
| UC-18 | 프라이버시 제어 (최초 동의, 일시정지, 음성 동의, 항목별 수집, 삭제) | `PrivacyStore`, `Recorder` | `TripCorrectionTest` 1건, 에뮬레이터 확인 | 앱 내부 | 구현 |
| M0 | Vehicle SDK 신호 조회와 변경 콜백 | `SeatProbeActivity` | 에뮬레이터 확인 | REAL | 구현 (확인용 화면) |
| UC-02, 04, 06, 13, 14 | P2 | 없음 | | | 미구현 |
| UC-08, 15, 17 | P3 | 없음 | | | 미구현 |

### 5.1 입력 출처별 구분

| 출처 | 해당 항목 |
|---|---|
| REAL | 주행 제한 상태 (CarUxRestrictions), 차량 신호 7종 ("실제 신호 기록" 선택 시. 3.4.1) |
| REPLAY | 차량 신호 전체 ("재생" 선택 시. 시나리오 CSV), 음성 인식 결과 |
| 미제공 | 음성 합성 출력, 위치, 지도 |

## 6. 플랫폼 확인 사항

에뮬레이터(Pleos Connect v2.1.0, API 34)에서 확인한 사실이다.

| ID | 확인 사항 | 영향 |
|---|---|---|
| PF-01 | 주행 제한 상태(기어 D)에서는 플랫폼이 앱 화면 전체를 "주행 중에는 사용할 수 없습니다" 화면으로 가린다 | 주행 중 화면을 쓰는 기능(UC-13, UC-06의 확인 표시)은 현재 불가. 가능 조건은 미확인 (open question 19) |
| PF-02 | Vehicle SDK 공식 문서에 속도, 기어, 시동 신호가 없다 | 트립은 착석, 도어, 거리로 추정하며 오판이 발생한다. UC-10이 필수 |
| PF-03 | STT, TTS, FusedLocation SDK는 문서상 준비 중이다 | 인터페이스만 구현하고 Fake로 대체 |
| PF-04 | 좌석 착석 값이 가상 콘솔의 표시와 반대로 수신된다 (Occupied일 때 false) | 원인 미확인 (open question 17). 현재는 에뮬레이터 관측에 맞춰 false를 착석으로 변환한다. 실차 확인 필요 |
| PF-05 | 주행 제한 전환은 가상 콘솔의 기어 선택으로 수행한다. 문서의 adb 명령은 동작하지 않는다 | 시험 절차 |
| PF-06 | "현재 트립 거리" 신호는 에뮬레이터에서 지원되지 않는다 (ERROR_NOT_SUPPORTED_PROPERTY) | 누적 주행거리(odometer)로 대체 |

## 7. 알려진 제한

| ID | 내용 |
|---|---|
| LM-01 | 트립 판정 임계값(일시정지 5분, 종료 확정 3분, 병합 후보 30분)과 관측 구간 최소 지속 시간은 가정값이다 |
| LM-02 | 기록 일시정지 중의 이동은 기록되지 않는다 |
| LM-03 | 트립 종료 시 진행 중이던 신호 상태를 초기화한다. 짧은 정차 직후의 트립에서 관측 구간이 누락될 수 있다 |
| LM-04 | DB 스키마 변경 시 기존 기록을 삭제한다 (개발 단계 설정) |
| LM-05 | 실제 신호의 값 해석 일부(착석, 도어, 창문, 등받이)는 에뮬레이터 관측에 근거한다. 실차에서 검증되지 않았다 |
| LM-06 | 트립 분할은 기록된 정차 지점에서만 가능하다. 임의 시각 분할은 지원하지 않는다 |
| LM-07 | 실제 신호 기록은 앱 화면이 살아 있는 동안에만 동작한다. 백그라운드 기록(서비스)은 없다 |

## 8. 빌드와 시험

| 항목 | 내용 |
|---|---|
| 빌드 도구 | Gradle 9.5.0, AGP 9.3.1, compileSdk 36, minSdk 28, targetSdk 35 |
| 단위 시험 | `./gradlew testDebugUnitTest` |
| 설치 | `./gradlew installDebug` (Pleos Connect Emulator 2.1.0, Cold boot) |
| 시나리오 | `jecheon_daytrip.csv` (트립 1, 관측 구간 6, 장면 후보 4), `errand_short_stop.csv` (트립 2로 오판 후 병합) |
| 신호 조작 | `adb forward tcp:8088 tcp:8088` 후 http://127.0.0.1:8088/ 의 Emulation Controls |
