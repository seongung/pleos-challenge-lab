# Pleos Journal v4.1 — controls and real-tap walkthrough

Functional build of `design-v4`. Interactive only: no autoplay, no `renderAt`, no prototype switcher, query params ignored. Storage key `pleosJournal.v4`.

Validated on the Android 15 emulator at 1920×1080: album/month filters, calendar navigation and selection, favorites, three-photo composition, sample dictation, family voice attachment, preview, save, reopening and persistence. A separate Claude designer reviewed actual emulator screenshots and approved the album, reader, favorites and corrected calendar. Headless interaction checks also passed without JavaScript errors.

## Handwriting revision

Nanum Pen Script is bundled offline for journal title, story, captions, family quotes and writing fields. All app navigation, dates, labels and controls retain the original sans font. The separate Claude designer reviewed the actual reader and composer screenshots. Recording plan: `android-tooling/record-plan-v4.1.json` (beach memory, composition, save, calendar replay). The older timing table below is a reference; the JSON is authoritative for this export.

## Helpers (WebView debugging)
- `window.journal.tap(selector)` → `{x,y}` centre in device pixels for `adb shell input tap`.
- `window.journal.state()` → screen, compose mode, draft contents, dictation/recording state, detected clip lengths.
- `window.journal.reset()` → restore the 5 sample journals without the dialog.
- `window.onAndroidBack()` → dialog → menu → photo viewer → compose (reorder → 직접 쓰기 → previous material → 닫기) → preview → where you came from → 앨범; returns `false` on 앨범.
- `window.demoReady === true` once photos have loaded.

## Top bar (앨범 · 달력 · 즐겨찾기)
| Visible | Selector | aria-label |
|---|---|---|
| Pleos Journal | `.brand` | — |
| 앨범 | `.tab[data-tab=album]` | 앨범 메뉴 |
| 달력 | `.tab[data-tab=cal]` | 달력 메뉴 |
| 즐겨찾기 N | `.tab[data-tab=fav]` | 즐겨찾기 메뉴 |
| search icon | `[data-act=search]` | 일기 찾기 |
| 새 일기 | `.bar [data-act=newEntry]` | 새 일기 만들기 |

## 앨범
- Today panel: `오늘의 일기 만들기` (`.today [data-act=newEntry]`) before saving; `오늘의 일기 펼쳐 보기` (`.today [data-act=open]`) after.
- Month tabs `.mo[data-m=all|2026-11|2026-10|2026-09|2026-08|2026-07]` — 전체 11월 10월 9월 8월 7월.
- Sort `[data-act=sort]` — 최신순 ↔ 오래된순. Count reads `일기 N편` (`#libCount`).
- Tiles `.tile[data-id=…]` (label `11월 8일 가을 소풍 도시락 열기`), hearts `.fav[data-id=…]` (`… 즐겨찾기에 넣기` / `에서 빼기`). The newest is the 2×2 feature tile.
- Sample ids: `s1108` 가을 소풍 도시락 · `s1018` 집에서 한 생일 파티 ♥ · `s0912` 비 오는 날의 쿠키 굽기 · `s0816` 바닷가 모래성 ♥ · `s0725` 숲속 캠핑.
- End cell `달력에서 찾기` (`.cell.end`) appears when the last row is one short (it does with 5 journals, not with 6).
- Search: the icon swaps the month row for a field `#fSearch` (찾을 말 입력); `[data-act=searchClose]` 찾기 닫기. Focusing it raises the keyboard.
- Scroll the mosaic to its end for `예시 데이터로 체험 중 · 처음 상태로` (`[data-act=reset]`; dialog 되돌리기 / 취소).

## 일기 (reader)
- `[data-act=back]` shows the origin: `앨범` / `달력` / `즐겨찾기`.
- Stepper `[data-act=step]` ×2 — 이전 일기 / 다음 일기, by date.
- Heart `.bar [data-act=fav]`, `고치기` (`[data-act=edit]`), `…` (`[data-act=more]`) → `삭제` (`[data-act=del]`, confirm).
- Photos on the page are buttons (`.pp[data-i=0..2]`, 사진 N 크게 보기) and open the full-screen viewer: `[data-act=viewerStep]` 앞 사진 / 뒤 사진, `[data-act=viewerClose]`.
- Voice block play button `.pbtn` (가족의 한마디 재생).

## 달력
- `[data-act=mprev]` 이전 달 · `[data-act=mnext]` 다음 달 · `[data-act=mtoday]` 오늘.
- Days `.cday[data-d=2026-10-18]`; days with a journal show its cover.
- Right panel for the selected day: its tile plus `이 날의 일기 펼쳐 보기` (`.daycol [data-act=open]`), or `이 날의 일기 만들기` / `오늘의 일기 만들기` (`[data-act=newOn]`).

## 즐겨찾기
Four-column mosaic of hearted journals; empty state `하트를 누른 일기가 여기에 모여요`.

## 일기 만들기 (one screen)
- Top: `닫기` (`[data-act=closeCompose]`, confirms if there is unsaved content) · title · `미리보기` (`#bPreview`).
- Left: the live page (not interactive) and the caption strip — caption fields `[data-cap=0..2]`; thumbnails `.sitem .th[data-i=N]`.
- Reorder: long-press a strip thumbnail (about half a second) to pick it up, then tap another thumbnail to drop it there. `adb shell input swipe x y x y 700` does the long-press.
- Dock tiles `.mode[data-m=photos|story|voice]` — 사진 · 이야기 · 한마디, any order.
- Footer `#bNext`: `다음 · 이야기` → `다음 · 가족의 한마디` → `완성된 일기 보기`; back circle beside it.
- **사진**: `.pk[data-id=lake|picnic|sunset]`, numbered in tap order, first is the cover; `내 사진 가져오기` opens the system picker.
- **이야기**: record button `#bDict` (받아쓰기 시작 / 받아쓰기 멈추기). The sample clip plays; words appear in the dock and on the page together, the newest word greyed. The title fills in when the first sentence completes. `직접 쓰기` (`[data-act=typing]`) makes title and story editable; `다시 말하기` (`[data-act=dictRedo]`) clears and restarts.
- **한마디**: `#bRec` (한마디 녹음 시작). When the clip ends the voice block is on the page with no further tap. `#bRec` then becomes 녹음 듣기; `다시 녹음` (`[data-act=recRedo]`), `지우기` (`[data-act=recClear]`).
- Preview: `고치기` back, `앨범에 저장` (`[data-act=save]`). Saving lands on 앨범 with the new journal as the feature tile, chip `방금 저장됨`.

## Audio
- Clip lengths are read from the files at runtime and each clip plays to its own end. Waveforms are computed from the audio when it can be decoded.
- Word timing follows the earlier clips' rhythm stretched to the real length, so it can drift a little against the new voices. For exact sync add `assets/voice-timings.json`: `{"dictation":{"text":"…","words":[{"w":"오늘은","t":0.43}]},"family-voice":{…}}`, or an ElevenLabs `alignment` object under each key. Supply `text` if the wording changed.

## Suggested recording (~125 s)
| Time | Tap | Shows |
|---|---|---|
| 0–7 | hold on 앨범 | Today panel with three photos waiting, five journals |
| 7–13 | `10월`, then `전체` | month filter |
| 13–22 | tile `집에서 한 생일 파티` | reader page |
| 22–27 | 이전 일기 arrow | stepping by date |
| 27–30 | `앨범` back | return |
| 30–38 | `달력`, `이전 달`, 18일 | covers on days, selected-day panel |
| 38–44 | `이 날의 일기 펼쳐 보기`, `달력` back | open from calendar |
| 44–52 | `즐겨찾기`, open `바닷가 모래성`, back | revisit a favourite |
| 52–56 | `앨범`, `오늘의 일기 만들기` | composer opens on 사진 |
| 56–66 | three photos | they land on the page one by one |
| 66–69 | `다음 · 이야기` | story material |
| 69–85 | `#bDict`, wait for the clip (12.7 s) | words on dock and page, title fills |
| 85–88 | `다음 · 가족의 한마디` | voice material |
| 88–95 | `#bRec`, wait (3.5 s) | voice block appears on the page by itself |
| 95–105 | `완성된 일기 보기`, play the voice note | full page preview |
| 105–109 | `앨범에 저장` | new feature tile, `방금 저장됨`, 일기 6편 |
| 109–114 | heart on the new tile | 즐겨찾기 count 2 → 3 |
| 114–125 | `달력` → 15일 → `이 날의 일기 펼쳐 보기` | reopen from the calendar |
