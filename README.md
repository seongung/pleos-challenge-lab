# Pleos Challenge Lab

Temporary repository for a two-person entry in the 2026 Hyundai Motor Group vehicle app challenge. Rename it after choosing the app.

**Status:** planning only. No Android app, working SDK integration, APK, or demo video exists yet.

[Project hub in Notion](https://app.notion.com/p/400d332dab56812c9fdfc93c05375ae7)

## Start here

1. Add your teammate under Settings → Collaborators.
2. Fill in team availability and roles in Notion.
3. Run the 45-minute brainstorming session and choose a lead idea plus a fallback.
4. Prove the riskiest SDK dependency by **October 24, 2026** using [docs/SDK-PROOF.md](docs/SDK-PROOF.md).
5. Choose the app by **October 25**. Proposed first candidate: Arrival Assist; this is not a team decision.

## Plan and research

- [Research and SDK feasibility](https://app.notion.com/p/400d332dab568102bd9af0d685116ca1)
- [Five concepts and brainstorming](https://app.notion.com/p/400d332dab5681f29647fa408e7ed400)
- [Timeline and tasks](https://app.notion.com/p/400d332dab5681c086b7e7266a8f0a1b)
- [Submission worksheet](https://app.notion.com/p/400d332dab56810a9d0fc0d84d121016)

Keep decisions and planning in Notion, technical bugs in GitHub Issues, and quick conversations in KakaoTalk.

## Deadlines

- Official application: **November 15, 2026, 23:59 KST** — confirmed in the [official application form](https://appdevchallenge.auto-hmg.io/downloads/contest-application-consent.docx).
- Proposed internal submission: **November 13, 18:00 KST**.
- First results: November 23. Offline PT: December 3–4; working app required before PT.
- Exact second-round material deadline: await organizer instructions.

[Official schedule](https://appdevchallenge.auto-hmg.io/guide/schedule) · [Submission guide](https://appdevchallenge.auto-hmg.io/guide/submission)

## Run and test environment

To be completed when an app exists.

Documented starting point: Android Studio, Kotlin/Java, minimum API 28, Gradle 8.0+, Pleos Connect Emulator 2.1.0 / API 34. Verify compatible versions at implementation time in the [setup guide](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator).

The SDK access guide lists Vehicle State and NaviHelper as immediately available; voice, FusedLocation, and vehicle-control access are marked coming soon. Verify project-specific access before relying on them. See [access guide](https://document.pleos.ai/docs/pleos-only/create-my-project).

## APK

No APK yet. Place the final executable release APK at:

`apk/팀명_앱이름_release.apk`

Use GitHub Releases when the file exceeds upload limits. Record the exact download link here and in the application. Private submissions require access for the organizer's announced reviewer account; confirm the account before inviting it.

## Contributing

Use a short branch per task and a pull request for review. Include what changed and how it was checked. Do not commit credentials, signing keys, personal consent forms, or participant contact details. Keep simulated data visibly distinct from real SDK results.

Before submitting linked materials, clarify the form's instruction about developer/account identity exposure, since GitHub repository URLs and commit attribution can identify their owner.
