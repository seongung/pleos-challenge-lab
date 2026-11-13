# Pleos Journal (가제)

현대자동차그룹 차량용 앱 개발 공모전(Pleos Playground) 출품작.
차가 이동 중 "기억의 단서"를 모으고, 정차 후 사용자가 음성 한마디로 의미를 붙이는 드라이브 저널 앱.

**현재 상태 (2026-11-14):** M0~M3 구현. Pleos Connect 에뮬레이터에서 시나리오 재생(REPLAY) 기반으로 P1 흐름이 동작한다.
신호와 음성 입력은 아직 재생, Fake이며 화면 배지로 구분한다. 실제 Vehicle SDK 연결은 좌석 착석 확인 화면(`SeatProbeActivity`)뿐이다.

[Notion 프로젝트 허브](https://app.notion.com/p/400d332dab56812c9fdfc93c05375ae7)

## 폴더 구조

```
CLAUDE.md                  Claude Code 작업 지침 (매 세션 자동 로드)
app/                       Android 앱 (Kotlin, Jetpack Compose, Room)
  src/main/java/com/pleosjournal/
    ui/                    View. 화면 담당이 고치는 곳
    presentation/          ViewModel과 View 계약(JournalUiState.kt)
    domain/                순수 Kotlin 로직
    data/ signal/ speech/ platform/
docs/
  01_contest.md            공모전 규정, 일정, 심사 기준
  02_strategy.md           입상 전략, 차별점, PT 예상 질문
  03_sdk_reference.md      Pleos SDK 가용성과 신호 목록
  04_usecases.md           유즈케이스 v2 (현재 기준)
  05_prototype_plan.md     아키텍처(정적 구조), 마일스톤, SIL 시나리오 설계
  06_open_questions.md     미확인 사항
  07_decision_log.md       결정 로그
  08_dev_overview.md       소프트웨어 아키텍처 설명서
  09_view_guide.md         View 계층 개발 매뉴얼
  DEMO-CHECKLIST.md        데모 인수 체크리스트
  SDK-PROOF.md             SDK 실현성 검증 양식
  screenshots/             에뮬레이터 화면 캡처
  reference/               원본 자료 (Notion Charter, Ideas, 외부 피드백, UC v1)
scenarios/                 SIL 시나리오 재생용 CSV (앱 assets로 포함됨)
apk/                       제출용 release APK 위치
```

## 빌드와 실행

요구 환경: Android Studio(JDK 포함), Pleos Connect Emulator 2.1.0(API 34).
에뮬레이터 설치: https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator

```bash
./gradlew testDebugUnitTest   # 단위 테스트 (도메인 로직, 계층 의존 규칙)
./gradlew installDebug        # 실행 중인 에뮬레이터에 설치
```

- 앱에서 "재생: 제천 당일치기" 또는 "재생: 편의점 경유"를 누르면 시나리오가 600배속으로 재생된다.
- 차량 신호와 기어(주행 제한) 변경: `adb forward tcp:8088 tcp:8088` 후 http://127.0.0.1:8088/ (Pleos Connect Virtual Console).
- 빌드 도구: Gradle 9.5.0, AGP 9.3.1, compileSdk 36, minSdk 28, targetSdk 35.

## 화면(View) 작업

- `ui/` 패키지만 고친다. 화면은 `JournalUiState`를 그리고 `JournalActions`만 호출한다.
- `ui/SampleState.kt`의 가짜 상태로 Android Studio 미리보기(@Preview)를 쓸 수 있다.
- 규칙 상세: `docs/05_prototype_plan.md`의 "정적 구조". 어기면 `ArchitectureTest`가 실패한다.

## APK

아직 release APK 없음. 최종 실행 파일 위치: `apk/팀명_앱이름_release.apk`
업로드 한도를 넘으면 GitHub Releases를 쓰고 링크를 여기와 지원서에 적는다.
비공개 저장소 제출 시 주최 측이 공지한 심사용 계정을 Collaborator로 초대한다(계정 확인 후 초대).

## 일정

- 접수 마감: **2026-11-15 23:59 KST**
- 1차 결과: 11-23. 오프라인 PT: 12-03 ~ 12-04 (PT 전까지 동작하는 앱 필수)

## 협업 규칙

- 결정과 기획은 Notion, 기술 버그는 GitHub Issues, 빠른 대화는 카카오톡.
- 작업별 짧은 브랜치와 Pull Request. 무엇을 바꿨고 어떻게 확인했는지 적는다.
- 자격 증명, 서명 키, 개인 동의서, 참가자 연락처를 커밋하지 않는다.
- 모의(재생, Fake) 데이터는 실제 SDK 결과와 화면에서 구분되게 한다.
- 제출 전 확인: 지원서의 개발자, 계정 신원 노출 관련 안내. 저장소 URL과 커밋 작성자로 소유자가 드러날 수 있다.
