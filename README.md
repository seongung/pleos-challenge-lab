# Pleos Journal (가제)

현대자동차그룹 차량용 앱 개발 공모전(Pleos Playground) 출품작.
차가 이동 중 "기억의 단서"를 모으고, 정차 후 사용자가 음성 한마디로 의미를 붙이는 드라이브 저널 앱.

## 폴더 구조

```
CLAUDE.md                  Claude Code 작업 지침 (매 세션 자동 로드)
docs/
  01_contest.md            공모전 규정, 일정, 심사 기준
  02_strategy.md           입상 전략, 차별점, PT 예상 질문
  03_sdk_reference.md      Pleos SDK 가용성과 신호 목록
  04_usecases.md           유즈케이스 v2 (현재 기준)
  05_prototype_plan.md     아키텍처, 마일스톤, SIL 시나리오 설계
  06_open_questions.md     미확인 사항
  07_decision_log.md       결정 로그
  reference/               원본 자료 (Notion Charter, Ideas, 외부 피드백, UC v1)
scenarios/                 SIL 시나리오 재생용 CSV
```

## 시작 방법

1. Android Studio에서 New Project → Empty Activity(Compose), Language Kotlin, Minimum SDK API 28 이상.
   Save location을 이 폴더로 지정한다(폴더가 비어 있지 않다는 경고가 나오면 그대로 진행).
   또는 Claude Code에게 "Pleos 샘플 앱 튜토리얼 기준으로 프로젝트 골격을 만들어줘"라고 요청해도 된다.
2. Pleos 에뮬레이터를 설치한다: `docs/03_sdk_reference.md`의 에뮬레이터 설치 문서 참고.
3. 이 폴더에서 Claude Code를 실행하고, 첫 요청으로 다음을 추천한다:
   > CLAUDE.md와 docs를 읽고, docs/05_prototype_plan.md의 M0를 진행하자. 먼저 필요한 공식 문서 확인 항목을 정리해줘.

## Git

- 원격 저장소: GitHub private `pleos-challenge-lab` (임시 이름)
- 연결 예: `git init && git remote add origin <저장소 URL>`
- 심사 제출 시 심사용 계정을 Collaborator로 초대
