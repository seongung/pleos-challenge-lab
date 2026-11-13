# 03. Pleos SDK 레퍼런스 (팀 조사 요약)

조사 시점: 2026-10 말. Vehicle SDK 문서 버전 v2.3.2 기준.
**코드 작성 시 클래스명, 메서드명, 퍼미션 문자열, 의존성 좌표는 반드시 아래 공식 문서에서 다시 확인할 것. 이 문서는 요약이며 정확한 API 시그니처를 담고 있지 않다.**

## 1. SDK 가용성

출처: https://document.pleos.ai/docs/pleos-only/create-my-project

| SDK | 문서상 상태 | 우리 계획 |
|---|---|---|
| Vehicle State (차량 상태 조회) | 사용 가능 (신청 불필요) | 실제 사용 |
| NaviHelper (내비 연동) | 사용 가능 (신청 불필요), 세부 기능 미확인 | 현재 UC에서는 미사용 |
| Vehicle Control (차량 제어) | 준비 중 | 현재 UC에서는 미사용 |
| TextToSpeech | 준비 중 | 인터페이스 구현 + Fake |
| SpeechToText | 준비 중 | 인터페이스 구현 + Fake |
| FusedLocation | 준비 중 | 인터페이스 구현 + Replay |

- **불일치 주의:** 공모전 FAQ는 5개 SDK 모두 쓸 수 있다고 나열하지만 Pleos 문서는 일부를 준비 중으로 표시. 공모전 참가자 별도 개방 여부 미확인.
- 외부 피드백에 따르면 STT/TTS 기본 HYBRID 모드는 서버를 사용할 수 있음(확인 필요).
- FusedLocation은 위치 좌표 API이며 지도 배경이나 장소명을 제공하지 않음. 외부 API 없이 지도를 그릴 수 있는지 미확인.

## 2. Vehicle SDK 구조

출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/intro

- 도메인별로 `capability 확인 → get → set → callback(변경 구독)` 구조.
- 퍼미션: 조회용 `CAR_*`, 제어용 `CONTROL_CAR_*` 계열. 정확한 문자열은 퍼미션 문서 확인.
  - https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-permission
- 차종마다 지원 여부가 달라 capability 확인 필수.

## 3. 조회 가능한 신호 (19개 도메인)

| 도메인 (퍼미션 계열) | 읽을 수 있는 신호 | 우리 UC 사용 |
|---|---|---|
| Brake (CAR_DRIVE) | 브레이크 페달 상태 | |
| DrivingMode (CAR_DRIVE) | 드라이브 모드, 크루즈 상태, 크루즈 목표 속도 | |
| Odometer (CAR_DRIVE) | 총 주행거리, 트립 거리(누적/현재/주유후), 트립 주행시간, 순간 주행거리 | UC-01, UC-14 |
| SteeringWheel | 조향각, 핸들 열선 | |
| TurnSignal (CAR_LIGHTS) | 좌우 방향지시등 | |
| Fuel (CAR_ENERGY) | 연료 잔량(%), 순간/현재/누적/주유후 연비 | UC-14 |
| EvBattery (CAR_ENERGY) | SoC, 충전 전류 제한, 충전 스위치, 전비 | UC-14 |
| Safety (CAR_SAFETY) | 에어백, 벨트 미착용 경고, 각종 오일/액 레벨, 경고등(Telltales) | |
| Tire (CAR_SAFETY) | 바퀴별 저압 경고 | |
| Seat (CAR_SEATS) | 좌석별 착석, 좌석별 벨트, **등받이 각도**, 앞뒤/높이/기울기, 헤드레스트, 열선/통풍 | UC-01, UC-02, UC-03 |
| Door (CAR_DOORS) | 도어 열림, 잠금, 글로브박스 | UC-01, UC-03 |
| Window (CAR_WINDOWS) | 창문 위치, 잠금, 블라인드, 스마트글라스 | UC-03 |
| Wiper (CAR_WINDOWS) | 와이퍼 작동, 스위치 | UC-03 |
| Light (CAR_LIGHTS) | 하향/상향등, 안개등, 미등, 실내등, 독서등, 앰비언트 | UC-03 |
| HVAC (CAR_CLIMATE) | 전원, AC, 풍량, 풍향, 설정 온도, **현재 실내온도**, 내기순환 등 | UC-03 |
| SideMirror (CAR_MIRRORS) | 사이드미러 열선 | |
| CarInfo (CAR_INFO) | VIN, CRN | (차량 구분용 후보, 프라이버시 주의) |
| Display (CAR_INFO) | 온도 표시 단위 | UI 표시 |
| Toll (CAR_TOLL) | 하이패스 음성 안내 음량 | |

## 4. 없는 신호 (중요)

- **차량 속도, 기어(P/D), 시동 상태: Vehicle SDK에 없음.** Android 표준 CarPropertyManager로 가능한지, 3rd party 허용인지 미확인.
- 주행/정차 구분: CarUxRestrictions 연동 가이드 존재. UX 제한 상태로만 판단.
- 외기온도, 습도, 선루프: 목록에서 찾지 못함.
- ADAS 상태(LFA, HDA 등): 크루즈 외 없음. Telltales 상세는 미확인.

## 5. 앱 심사, UX 관련 문서

- 앱 심사(SDK, API): https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/review-functionality-performance-stability
- 운전자 주의분산 설계: https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/review-driver-distraction-elements
- 앱 개발 가이드: https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/intro
- 에뮬레이터 설치: https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator
- Vehicle SDK 샘플 앱 튜토리얼: https://document.pleos.ai/docs/pleos-only/tutorial/vehicle-sdk-sample-app

## 6. 프로젝트 설정 시작점

Gradle 의존성 좌표, 저장소 URL, AndroidManifest 설정은 위 "에뮬레이터 설치"와 "샘플 앱 튜토리얼" 문서를 따른다. 이 저장소의 어떤 문서도 그 값을 확정하지 않는다.
