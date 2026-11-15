# Pleos Journal — design v4

Visual contract: `design-v4/index.html` (authored at 1920×1080, scales to fit).
Brand is exactly **Pleos Journal**. Feature labels and copy are Korean. `가족 일기` is not used anywhere.

| State | URL |
|---|---|
| Album, before today's journal | `?screen=album` |
| Album, after saving | `?screen=album&saved=1` |
| Compose: photos / story / voice | `?screen=compose&step=photos` · `step=story` · `step=voice` |
| Journal, reading | `?screen=journal` |
| Journal, preview before save | `?screen=journal&mode=preview` |

Without `?screen=` a small `album | compose | journal` pill appears at the bottom centre. It is a prototype aid only: **it must not exist in the app**, and it is hidden whenever `?screen=` is given (use those URLs for clean captures). Buttons inside the screens also navigate (만들기 → compose → 완성된 일기 보기 → 앨범에 저장 → album).

## 1. What was wrong with v3

1. **Chrome outweighed content.** A 284 px sidebar held three links and a button. In landscape that costs 15% of the width on every screen.
2. **Everything was the same beige.** Background, sidebar, cards and panels sat within a few percent of each other, so nothing had priority and photos looked washed out.
3. **The library was a uniform card grid.** Five identical cards, each half photo and half white label box, left 40% of the screen empty. Dates were small grey text although date is how people look for a memory.
4. **Composing was a form wizard.** Four numbered steps, each with large empty bordered panels. The user never saw the journal take shape until step 4.
5. **The journal was a detail view.** Photo on the left, metadata list on the right, voice in a boxed card. Nothing about it read as a page worth keeping.
6. **Type had no system.** Serif titles, sans labels and sans body mixed at similar sizes; hierarchy came from boxes instead of scale.

## 2. Direction: evening table, paper page

The app is a dark, quiet surface. The journal is the one bright object on it.

- **Dark canvas** for everything that is navigation or tooling. Photos keep their colour against it and the chrome recedes.
- **Paper** only for the journal page itself. The same page component appears while composing (scaled down, filling in live) and when reading (full size). What you build is what you keep.
- **Dates are the typography.** A large thin numeral for the day appears on the Today panel, on every album tile and on the page masthead. It is the one recurring graphic device.
- **One signal colour**, persimmon, for three meanings only: today, recording in progress, favourite. Primary actions are paper-coloured buttons, not accent-coloured.
- **Sans only.** Hierarchy comes from size and weight (200 numerals, 700 titles, 400–600 text). No serif.
- **No cards with label boxes.** Album tiles are full-bleed photos with text over a bottom scrim. Framed surfaces are limited to the compose dock and the caption strip.

## 3. Tokens

### Colour
| Token | Value | Use |
|---|---|---|
| `--bg` | `#0E0F11` | App canvas |
| `--s1` | `#17181B` | Dock, ghost buttons, caption items |
| `--s2` | `#212327` | Active tab, mode tiles, image placeholders |
| `--s3` | `#2E3136` | Active mode tile |
| `--line` / `--line2` | white 9% / 16% | Hairlines, field underlines |
| `--t1` / `--t2` / `--t3` | `#F3EFE6` / `#B4AFA4` / `#7C786F` | Text on dark: primary / secondary / inactive |
| `--paper` | `#F6F1E7` | Journal page, primary buttons, photo print borders |
| `--paper2` / `--paper3` | `#EAE3D4` / `#DCD3C1` | Voice block on page / page rules and dashed slots |
| `--ink` / `--ink2` / `--ink3` | `#1B1A17` / `#5A544A` / `#958D80` | Text on paper |
| `--sig` | `#FF6A3D` | Today dot, recording, favourite, "방금 저장됨" |

### Type (system Korean sans: Noto Sans CJK KR on Android)
| Role | Size / line | Weight |
|---|---|---|
| Day numeral: Today panel / page / feature tile / tile | 196 / 136 / 128 / 64, line 0.8 | 200 (300 on tiles) |
| Page title | 54 / 1.22 | 700 |
| Feature tile title / month tabs | 40 / 30 | 700 |
| Page story | 28 / 1.78 | 400 |
| Dock transcript | 25 / 1.62 | 400 |
| Dock heading, tile title, tab | 24–26 | 700 |
| Body, buttons | 22 | 600–700 |
| Secondary, captions, chips | 17–20 | 500–600 |

Numerals use tabular figures and −0.04 to −0.06em tracking. `word-break: keep-all` everywhere. If the device font has no weight 200, weight 300 is acceptable; do not substitute a serif.

### Space, radius, targets
- Outer margin 56. Top bar 88. Content starts at y 112 (104 for the reader). Gaps 16 between tiles, 24 between regions.
- Radius: 14 (photos on the page), 20 (tiles, strip items), 28 (page, dock, feature tile), pill (buttons, tabs, chips).
- Touch targets: 64 minimum. Primary actions 76–80. Record button 104. Tile heart 60.
- Motion: 320 ms `cubic-bezier(.2,.8,.2,1)`. Screens enter with a 14 px rise and fade. Pressed controls scale to 0.97.

## 4. Navigation

Top bar on the three main sections:
`Pleos Journal` wordmark · tabs **앨범 · 달력 · 즐겨찾기 (count)** · search · **새 일기**.

Compose and journal replace the tabs with a single back or close control and a centred title, so the task has the whole screen. Android back follows the same path: dialog → compose step → preview → where the user came from → 앨범.

`처음 상태로` (demo reset) leaves the main chrome. It becomes one quiet text row at the very end of the album scroll, below the mosaic: `예시 데이터로 체험 중 · 처음 상태로` in `--t3`, 64 high, with the same confirm dialog as v3. It is a demo control, not a product feature.

Every control drawn must work. Nothing is drawn for sharing, settings or accounts.

## 5. Screens

### 5.1 앨범 (`?screen=album`)
- **Today panel**, x 56–560. Signal dot + `오늘`, numeral `15`, `11월 / 일요일 · 2026`, a fanned stack of today's three photos with paper borders, then one line of status and one button.
  - Before: `오늘 찍은 사진 3장` / `아직 오늘의 일기가 없어요` / paper button **오늘의 일기 만들기**.
  - After: `오늘의 일기를 남겼어요` in signal colour / ghost button **오늘의 일기 펼쳐 보기**.
- **Library**, x 616–1864.
  - Header: thin `2026`, month tabs as large type (`전체 11월 10월 9월 8월 7월`, active is `--t1`), `일기 N편`, sort toggle.
  - Mosaic: 3 columns, rows of 272, gap 16. The newest entry is a feature tile spanning 2×2 (788×560). All others are 386×272. Both are close to 3:2, so the supplied photos crop cleanly.
  - Tile: full-bleed cover, bottom scrim, day numeral, `월 · 요일`, title. Top-left chips for photo count and voice length. Top-right heart. The feature tile also shows one line of story.
  - After save, today's journal becomes the feature tile with a `방금 저장됨` chip and the previous feature drops into the grid. Nine cells are then exactly full.
  - End cell: when the last row would end one cell short, a dashed cell **달력에서 찾기 / 날짜로 지난 일기 보기** closes the row and opens 달력. It is not shown otherwise.
  - Heart sits on a 48 px dark disc so it stays visible on bright or warm photos.
- Month tab filters the mosaic in place. Search opens a field over the header and filters live.

### 5.2 일기 만들기 (`?screen=compose`)
One editor instead of four pages.

- **Page canvas**, x 56–1304, y 112–769. The real journal page at 0.69 scale. Empty parts show dashed slots and grey placeholder text. Every input updates it immediately.
- **Caption strip**, below the canvas. One item per photo: thumbnail with order number, `표지` / `사진 2` label, caption field. Tap to edit, long-press to reorder.
- **Dock**, x 1328–1864.
  - Three peer mode tiles: **사진 · 이야기 · 한마디**. Each shows its own status (`3장`, `받아쓰는 중`, `0:04`). The photo tile takes the cover as its background once chosen. Any tile can be tapped at any time; order is suggested, not enforced.
  - **사진**: 2-column grid of today's photos with order badges, plus `내 사진 가져오기`. `예시 사진` chip stays for honesty about sample content.
  - **이야기**: title field, live transcript, 104 px record button with live waveform and timer, `직접 쓰기` (makes title and story editable with the keyboard) and `다시 말하기` (clears the dictated text and starts again). Words not yet final are shown in `--t3`. The title fills in from the first sentence if empty.
  - **한마디**: record, then the clip is on the page automatically. v3's separate `기록에 첨부` tap is removed. Afterwards the panel shows the quote, a play button, `다시 녹음` and `지우기`.
  - Footer: back circle + one paper button: `다음 · 이야기` → `다음 · 가족의 한마디` → `완성된 일기 보기`.
- Top bar: `닫기` (confirm if there is content), `오늘의 일기 · 11월 15일 일요일`, `미리보기`.

### 5.3 일기 (`?screen=journal`)
The page at full size, 1808×952, on the dark canvas.

- **Left, 1060 wide**: cover photo 1060×520, then two photos 522×264. Each has a numbered caption line on the paper beneath it (`01 낙엽을 따라 걷던 길`).
- **Right, 620 wide**: masthead (numeral, month, weekday · year, counts), rule, title, story, and the voice block pinned to the bottom: label, quote, play button, waveform, length.
- Masthead counts are one line, right-aligned on the numeral's baseline, never wrapped: `사진 3장 · 음성 0:04` (`white-space: nowrap`). The title may run to two lines; a third is truncated.
- Photo count rules: 1 photo → cover fills the whole left column. 2 → cover plus one full-width 1060×264. 3 → as drawn. 4 or more → third slot shows `+N` and opens a full-screen viewer. Tapping any photo opens that viewer.
- No voice → the voice block is omitted and the story takes the space.
- **Reading bar**: `앨범` back (label names the origin: 앨범 / 달력 / 즐겨찾기), a centred stepper to the previous and next journal by date, heart, `고치기`, overflow with `삭제`.
- **Preview bar** (`&mode=preview`): `고치기` back, `미리보기 · 저장하면 앨범 맨 앞에 놓여요`, paper button **앨범에 저장**. Saving returns to 앨범 with the new feature tile.

### 5.4 달력 and 즐겨찾기 (not drawn; build from the same parts)
- **달력**: same top bar. Left 1192 wide: month title as large type with previous/next, 7-column grid of 160×132 cells, gap 8, radius 14. A day with a journal is filled by its cover with the day numeral over a scrim. Empty days are `--s1` with a `--t3` numeral; future days have no fill. Today has a 2 px signal ring. Right 560 wide: the Today-panel layout reused for the selected day (numeral, month, weekday, then either a tile for that day's journal or `이 날의 일기 만들기`).
- **즐겨찾기**: the album mosaic without the Today panel, 4 columns across the full width, first tile 2×2. Empty state: one line, `하트를 누른 일기가 여기에 모여요`.

## 6. Flow for the recording

1. 앨범 opens. Today panel shows three photos waiting; library shows five journals.
2. Tap a month tab and back; open a tile; step to the previous journal; return.
3. 달력 → a day with a cover → open → back. 즐겨찾기 → open → back.
4. **오늘의 일기 만들기**. Tap three photos; they land on the page one by one.
5. **다음 · 이야기**. Tap record; words appear in the dock and on the page together; title fills in.
6. **다음 · 가족의 한마디**. Tap record; the voice block appears on the page when the clip ends.
7. **완성된 일기 보기** → full page → play the voice note → **앨범에 저장**.
8. 앨범: today's journal is the feature tile, `방금 저장됨`. Tap its heart; 즐겨찾기 count goes up.

## 7. Implementation guidance

- Keep v3's data, storage, audio timing, file chooser and `window.journal` helpers. Replace markup and CSS; do not restyle the old structure.
- Copy the `:root` tokens and the `.page` block from `index.html` unchanged. Build the page once and use it in three places: compose canvas (`transform: scale(.69027)`, origin top-left, inside a 1248×657 box), preview, reader.
- The prototype loads photos from `../family-journal-v3/assets/`. The app must copy them and use `assets/` relative paths.
- Photo crops use `object-fit: cover` plus a per-photo `object-position`. Values for the seven sample photos are in the `P` and `E` tables in `index.html`. Imported photos default to `50% 40%`.
- Fixed 1920×1080 layout. If the WebView reports another size, scale the stage as the prototype does rather than reflowing.
- Selectors the recording scripts rely on can keep their ids; map them onto the new elements (`#bDict` and `#bRec` → `.recbtn`, `#bNext` → dock footer button, `.tile[data-id]` → `.pk`, `.card .open` → `.tile`, `.card .fav` → `.fav`).
- `backdrop-filter` on chips is optional; the 56% dark fill is enough without it.
- Waveforms: reuse v3's decoded envelopes. Bars are 6 px max with 4 px gaps; played bars switch from `--ink3` to `--ink`.
- Do not add: sidebar, step numbers, bordered form panels, serif, a second accent colour, statistics, onboarding or marketing copy.

## 8. Status — design frozen

All seven states were rendered by the parent and reviewed by the designer. The design is frozen at this point; implementation follows `index.html` and this document.

Corrected after the first render (confirmed fixed in the second):
1. Masthead counts wrapped one character per line. Now one line: `사진 3장 · 음성 0:04`.
2. Page title raised to 54 and story to 28. The sample title fits one line at full size.
3. Tile numerals and the favourite heart are legible on bright photos (deeper scrim, weight 400, dark disc behind the heart).
4. The blank ninth album cell is replaced by the 달력에서 찾기 end cell.

Corrected after the second render (small CSS fixes, not re-rendered):
5. The prototype switcher stayed visible with `?screen=` because `display:flex` beat the `hidden` attribute. Added `#sw[hidden]{display:none}`. The app must not contain the switcher at all.
6. Saved album: the check mark sat on its own line above `오늘의 일기를 남겼어요`. It is now inline, to the left of the text.

Notes for the implementer and for APK review:
- In the second set of renders the paper looks grey (`#DBD8D0`-ish) instead of `#F6F1E7`, and paper buttons look dull. That is the 320 ms screen-enter fade caught mid-way, not a colour change. Capture after the transition ends; the first `journal.png` render shows the correct paper.
- On the journal page, the space between a short story and the voice block is intentional: the voice block is pinned so its bottom edge aligns with the bottom photo row. Do not stretch the story or centre the block to fill it.
- In the compose dock, the transcript area is sized for the full three-sentence story; it looks sparse only at the start of dictation.

Review checklist for APK screenshots: brand reads `Pleos Journal`; no sidebar, serif or beige surfaces; paper is `#F6F1E7`; day numerals are thin and large; counts on one line; tiles near 3:2 with faces uncropped; every control at least 64 px; no switcher pill.
