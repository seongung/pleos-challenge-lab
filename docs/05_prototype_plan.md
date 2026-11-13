# 05. 초기 프로토타입 계획

## 목표

1. 가장 큰 기술 리스크(에뮬레이터에서 Vehicle SDK 신호를 실제로 읽을 수 있는가)를 먼저 검증한다.
2. P1 핵심 흐름을 시나리오 재생으로 끝까지 한 번 돌린다.
3. 이후 실제 SDK 소스로 하나씩 교체한다.

## 마일스톤

| 단계 | 내용 | 완료 기준 | 관련 UC |
|---|---|---|---|
| M0 | Android 프로젝트 생성, Pleos 에뮬레이터 실행, Vehicle SDK 신호 1개(예: 운전석 착석) 조회와 변경 콜백 수신 | 에뮬레이터에서 값 변경 시 로그 출력 | 리스크 검증 |
| M1 | 신호 추상화 계층 + ReplaySource(CSV) + TripDetector + ClueExtractor + Room 저장 + 단순 타임라인 | `scenarios/jecheon_daytrip.csv` 재생 시 트립 1개와 단서 구간이 타임라인에 표시 | UC-01, 03, 11 |
| M2 | SceneSuggester + 장면 확인 화면 + FakeStt 기반 음성 회고 | 트립 종료 후 질문이 나오고, Fake 답변이 저장됨 | UC-12, 07b |
| M3 | DrivingGate(UX 제한), 트립 보정, 프라이버시 최소 화면, 출처 배지 | 주행 상태에서 입력 UI가 닫히고, 병합/삭제와 수집 중지 동작 | UC-09, 10, 18 |
| M4 | 실제 PleosVehicleSource 연결 범위 확대, 데모 영상 녹화 | 실제 신호 구간과 SIL 구간이 화면 배지로 구분된 영상 | 전체 |

일정 참고: 1차 서류 마감 11-15, 2차 PT 12-03. 서류 전 M0~M1까지 되면 영상과 저장소를 가산점용으로 첨부, 안 되면 목업 영상으로 대체하고 PT까지 M4 완료.

## 패키지 구조 (제안)

```
app/src/main/java/<base package>/
  signal/            # 신호 소스 인터페이스와 구현
    VehicleSignalSource.kt
    VehicleEvent.kt            # sealed class: SeatOccupancy, Door, Window, Wiper, Headlamp, SeatRecline, Hvac, TripDistance ...
    pleos/PleosVehicleSource.kt
    replay/ReplaySource.kt     # CSV 재생, 재생 속도 배율 지원
    dummy/ManualDummySource.kt # 디버그 화면에서 이벤트 주입
  speech/            # SpeechSource 인터페이스, PleosStt, FakeStt / TTS 동일
  location/          # LocationSource 인터페이스, PleosFusedLocation, ReplayLocation
  domain/
    TripDetector.kt            # 상태 머신 (IDLE → CANDIDATE → ACTIVE → PAUSED → ENDED)
    ClueExtractor.kt           # 이벤트 → 관측 구간(ObservedSpan)
    SceneSuggester.kt          # 관측 구간 조합 → 장면 후보 + 질문
    DrivingGate.kt             # UX 제한 상태 → 입력 허용 여부 (unknown = 차단)
  data/              # Room entity, dao, repository
  ui/                # timeline, scene, recall, settings, debug
```

- 신호 이벤트는 공통 타입(`VehicleEvent`)으로 정규화한 뒤 도메인으로 보낸다. 실제 SDK 타입이 도메인으로 새어 들어가지 않게 한다.
- 모든 이벤트에 `source: REAL | REPLAY | DUMMY` 필드를 둔다. UI 배지와 저장 데이터 모두에 남긴다.

## TripDetector 초안 (조정 대상)

- 시작 후보: 운전석 착석 → 트립 거리 증가 감지 시 ACTIVE.
- 일시정지: 일정 시간 거리 변화 없음 → PAUSED.
- 종료: PAUSED 상태에서 운전석 도어 열림 + 운전석 착석 해제 + 일정 시간 경과 → ENDED.
- 재개: 종료 판정 전 거리 다시 증가 → ACTIVE (같은 트립).
- 사후 병합: 종료 후 짧은 간격 안에 새 트립 시작 시 병합 후보로 표시(사용자 보정 UC-10).
- 주의: 이 결과는 기록용이다. UX 제한 해제나 안전 판단에 쓰지 않는다.
- 시험 시나리오: 신호 대기, 동승자만 하차, 편의점 짧은 경유, 주차장 재주차, 충전 대기.

## 시나리오 CSV 형식 (SIL)

```
t_ms,signal,target,value
0,SEAT_OCCUPIED,DRIVER,true
1000,TRIP_DISTANCE_KM,,0.0
...
```

- `#`으로 시작하는 행은 주석이며 파서가 건너뛴다.
- `t_ms`: 시나리오 시작 기준 밀리초. 재생 배율(예: 60배)로 긴 여정을 짧게 재생.
- `signal`: 우리 앱 내부 신호 이름(SDK 이름이 아님). 매핑은 `PleosVehicleSource`에서 담당.
- `target`: 좌석, 도어, 창문 위치 등 대상. 없으면 빈칸.
- 위치, 음성도 같은 파일에 `LOCATION`, `SPEECH` 행으로 넣어 하나의 시나리오로 재생 가능.
- 예시: `scenarios/jecheon_daytrip.csv` (축약본, 값은 가상)

## 디버그 화면 (데모와 개발 공용)

- 현재 소스 선택(REAL / REPLAY / DUMMY)
- 시나리오 선택, 재생, 일시정지, 배율
- 수동 이벤트 주입 버튼(와이퍼 on/off, 창문 열기, 뒷좌석 젖힘 등)
- 실시간 이벤트 로그

## AI 코딩 작업 분할 원칙

- 한 프롬프트 = 한 컴포넌트 + 그 테스트. 예: "TripDetector 상태 머신과 시나리오 5개 테스트".
- 순서: 인터페이스와 이벤트 타입 → ReplaySource → 도메인 로직(테스트 먼저) → Room → UI → 실제 SDK 소스.
- SDK 연동 프롬프트에는 반드시 해당 공식 문서 URL을 함께 주고, 문서에서 확인한 시그니처만 쓰게 한다.
