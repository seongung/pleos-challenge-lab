# 06. 미확인 사항과 확인 작업

| # | 질문 | 확인 방법 | 영향 | 상태 |
|---|---|---|---|---|
| 1 | Pleos Playground 프로젝트 생성 후 각 SDK의 실제 상태(신청 가능 여부) | 가입 → 프로젝트 생성 → SDK 메뉴 캡처 | SIL 범위 | 미확인 |
| 2 | 에뮬레이터에서 Vehicle SDK 조회와 콜백이 동작하는가, 신호를 에뮬레이터에서 바꿀 수 있는가 | M0 작업 | 전체 데모 | **확인 완료 (2026-11-13)**: Apple Silicon Mac + Pleos Connect v2.1.0 이미지(API 34)에서 Seat occupancy capability, get, 변경 콜백 모두 동작. 신호 변경은 Virtual Console(`adb forward tcp:8088 tcp:8088` 후 http://127.0.0.1:8088/ 의 Emulation Controls). 버튼 조작 후 콜백은 약 0.1초 내 도착. capability 좌석은 ROW_1_LEFT/RIGHT, ROW_2_LEFT/CENTER/RIGHT |
| 3 | 준비 중 SDK(STT, TTS, FusedLocation, Control)를 에뮬레이터에서 호출할 수 있는가 | 에뮬레이터 시험 | SIL 범위 | 미확인 |
| 4 | STT/TTS HYBRID 모드의 서버 사용 여부, 온디바이스 옵션 | STT/TTS 문서 | 기획서 표현 | 미확인 |
| 5 | 외부 API 없이 지도 표시 가능 여부 | Pleos 문서, NaviHelper 기능 목록 | UC-11 지도 | 미확인 |
| 6 | CarUxRestrictions 연동 방법과 에뮬레이터에서의 상태 전환 | 앱 개발 가이드, 주의분산 문서 | UC-09 | 미확인 |
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
| 17 | Virtual Console의 Seat Occupancy에서 Driver를 "Occupied"로 하면 SDK 콜백 `ROW_1_LEFT`는 `false`, "Empty"로 하면 `true`가 온다(2026-11-13 반복 재현). SDK Boolean의 의미가 반대인지, 콘솔 쪽 버그인지 | Seat 문서 본문, 다른 좌석으로 재현, 문의 메일 | UC-01, UC-02 착석 판정 (PleosVehicleSource 매핑) | 미확인. 확인 전까지 매핑을 한 곳에서만 하고 추측으로 뒤집지 않는다 |
| 18 | 에뮬레이터 이미지에 `ai.pleos.playground.service.location.fused`, `...service.navi`, `...service.adas` 패키지가 설치돼 있음. 준비 중 SDK의 클라이언트 라이브러리 좌표와 호출 가능 여부 | 공식 샘플(pleos-sample-fusedlocation-app) 확인 | 3번 질문, SIL 범위 | 미확인 |
