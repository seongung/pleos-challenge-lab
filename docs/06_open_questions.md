# 06. 미확인 사항과 확인 작업

| # | 질문 | 확인 방법 | 영향 | 상태 |
|---|---|---|---|---|
| 1 | Pleos Playground 프로젝트 생성 후 각 SDK의 실제 상태(신청 가능 여부) | 가입 → 프로젝트 생성 → SDK 메뉴 캡처 | SIL 범위 | 미확인 |
| 2 | 에뮬레이터에서 Vehicle SDK 조회와 콜백이 동작하는가, 신호를 에뮬레이터에서 바꿀 수 있는가 | M0 작업 | 전체 데모 | **확인 완료 (2026-11-13)**: Apple Silicon Mac + Pleos Connect v2.1.0 이미지(API 34)에서 Seat occupancy capability, get, 변경 콜백 모두 동작. 신호 변경은 Virtual Console(`adb forward tcp:8088 tcp:8088` 후 http://127.0.0.1:8088/ 의 Emulation Controls). 버튼 조작 후 콜백은 약 0.1초 내 도착. capability 좌석은 ROW_1_LEFT/RIGHT, ROW_2_LEFT/CENTER/RIGHT |
| 3 | 준비 중 SDK(STT, TTS, FusedLocation, Control)를 에뮬레이터에서 호출할 수 있는가 | 에뮬레이터 시험 | SIL 범위 | 미확인 |
| 4 | STT/TTS HYBRID 모드의 서버 사용 여부, 온디바이스 옵션 | STT/TTS 문서 | 기획서 표현 | 미확인 |
| 5 | 외부 API 없이 지도 표시 가능 여부 | Pleos 문서, NaviHelper 기능 목록 | UC-11 지도 | 미확인 |
| 6 | CarUxRestrictions 연동 방법과 에뮬레이터에서의 상태 전환 | 앱 개발 가이드, 주의분산 문서 | UC-09 | **확인 완료 (2026-11-14)**: `CarUxRestrictionsManager`로 연동. 에뮬레이터 전환은 Virtual Console의 Gear Selection(P는 제한 없음, D는 DO=true). 문서의 `adb shell cmd hmg_car_service ...` 명령은 에뮬레이터 v2.1.0에서 "Can't find service"로 실패 |
| 7 | 타 앱 재생 음악 정보(MediaSession) 접근 권한 | 문서, 시험 | UC-04 | 미확인 |
| 8 | 음성 호출 경로(앱이 포그라운드 아닐 때 "지금 기억해줘") | 문서 | UC-06 | 미확인 |
| 9 | Pleos 공식 폰 연동 기능 존재 여부 | 문서 | UC-16 재추가 | 미확인 |
| 10 | 공모전: 준비 중 SDK 개방 여부, 더미/SIL 시연 허용 범위, 2차 시연 환경(에뮬레이터 vs 실차) | 문의 메일 appdevchallenge@hyundai-partners.com | 전략 | 미발송 |
| 11 | 온라인 설명회(10-28) 다시보기 시청 | 공지 페이지 | 전략 | 미시청 |
| 12 | Pleos App Market 유사 앱 조사 | 마켓 확인 | 창의성 | 미확인 |
| 13 | 팀원 재직사 겸업, IP 규정 | 각자 사내 규정 | 참가 자격 | 미확인 |
| 14 | 팀 정보(팀명, 대표자, 주당 투입 시간, 역할 분담) | 팀 회의 | 제출 | 미정 |
| 15 | 사업성 서술 보강 | 팀 작업 | 1차 심사 | 미완 |
| 16 | Vehicle SDK 2.3.2 AAR에 `Gear` 도메인(`Vehicle.getGear()`, `getCurrentGearSelectionState`, `registerGearSelection`)이 들어 있음. 공식 문서의 Vehicle 멤버 목록에는 없음. 3rd party 사용 가능 여부, 필요한 퍼미션, 에뮬레이터 동작 | 에뮬레이터 시험, 문의 메일 | UC-01 트립 판정 정확도, CLAUDE.md 제약 3 | 미확인 (확인 전까지 제약 3 유지) |
| 17 | Virtual Console의 Seat Occupancy에서 Driver를 "Occupied"로 하면 SDK 콜백 `ROW_1_LEFT`는 `false`, "Empty"로 하면 `true`가 온다(2026-11-13 반복 재현). SDK Boolean의 의미가 반대인지, 콘솔 쪽 버그인지 | Seat 문서 본문, 다른 좌석으로 재현, 문의 메일 | UC-01, UC-02 착석 판정 (PleosVehicleSource 매핑) | 미확인. 확인 전까지 매핑을 한 곳에서만 하고 추측으로 뒤집지 않는다 2026-11-14: `PleosVehicleSource`는 에뮬레이터 관측에 맞춰 false를 착석으로 변환한다(상수 `SEAT_OCCUPIED_RAW`). 실차나 공식 답변으로 확인되면 그 상수만 고친다 |
| 18 | 에뮬레이터 이미지에 `ai.pleos.playground.service.location.fused`, `...service.navi`, `...service.adas` 패키지가 설치돼 있음. 준비 중 SDK의 클라이언트 라이브러리 좌표와 호출 가능 여부 | 공식 샘플(pleos-sample-fusedlocation-app) 확인 | 3번 질문, SIL 범위 | 미확인 |
| 19 | 주행 제한(기어 D)이 걸리면 플랫폼이 앱 화면 전체를 "주행 중에는 사용할 수 없습니다" 화면으로 가린다(2026-11-14 에뮬레이터 확인, `docs/screenshots/driving_blocked_by_platform.png`). 주행 중에도 화면을 보이려면(예: UC-13 재방문 회상의 읽기 전용 화면) distraction optimized 선언이 필요한 것으로 보이나, Pleos에서의 선언 방법과 심사 조건 | 주의분산 설계 문서, UI Design Guide, 문의 메일 | UC-09, UC-06, UC-13. 현재 앱 안의 게이트는 플랫폼 차단 아래의 2차 방어로만 동작 | 미확인. 확인 전까지 선언하지 않는다 |
| 20 | 실제 신호 값의 의미와 단위: 도어 위치(0이 닫힘인가), 창문 위치(0~100이 열림 정도인가), 등받이 각도 위치(단위, 젖힘 기준), 좌핸들과 우핸들의 좌석 대응. 공식 문서의 메서드 설명에 값의 의미가 없다 | 문의 메일, 실차 또는 DevBox 시험 | UC-01, UC-03의 실제 신호 정확도 | 미확인. 현재 값은 에뮬레이터 관측과 가정 |
| 21 | `Odometer.registerTripCurrentDrivingDistance`가 에뮬레이터 v2.1.0에서 ERROR_NOT_SUPPORTED_PROPERTY를 반환한다(2026-11-14). 실차에서 지원되는지 | 문의 메일, 실차 시험 | UC-01, UC-14. 현재는 누적 주행거리(`registerDistanceDriven`, km)로 대체 | 미확인 |
