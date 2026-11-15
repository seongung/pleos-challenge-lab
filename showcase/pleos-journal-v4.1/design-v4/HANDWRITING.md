# Pleos Journal — handwriting for the journal's own words

Scope of this note: typography only. The approved layout, colours and all UI type stay as they are. Target file: `family-journal-v4.1/index.html` (v4 stays untouched). Nothing here has been applied; this is the recommendation for the implementer.

## Choice: Nanum Pen Script (나눔손글씨 펜)

- Noonnu listing: https://noonnu.cc/font_page/44 (`나눔손글씨펜`, NAVER, shown as OFL: modify, copy and redistribute allowed, selling the font file itself is not).
- File already downloaded by the parent: `design-v4/font-candidates/NanumPenScript-Regular.ttf` with `OFL-NanumPen.txt`.
- One weight only (Regular).

Why this one:
- It is real handwriting: uneven baseline, pen pressure, joined strokes. The user asked for a handwriting font, and this reads as one at a glance.
- It stays legible. In the rendered sample every syllable of the story is clear at 38 px, including dense ones (`걸었어요`, `않고`, `좋았어요`).
- OFL from NAVER, so it can be bundled in the APK offline with the licence text. No attribution ambiguity.

Why not the others:
- **NanumBarunPen** (font_page/42): very readable, but in the sample it looks like a tidy printed pen face, not someone's writing. The title and quote only looked heavier because the browser faked a bold; there is no bold file at the Noonnu CDN. Keep it as the fallback if the user finds Pen Script too casual.
- **Nanum Brush Script** (font_page/43): not judged. It was cut off in the comparison render; brush faces are usually harder to read at caption size, which is an expectation, not something I saw.
- **KCC은영체**: licence wording is contradictory per the parent's check. Skipped.
- **Kyobo**: redistribution banned. Skipped.

Samples (rendered from the actual font files at the real column widths):
- `design-v4/handwriting-sample.png` — Nanum Pen Script on the paper text column (620 px) and on the dark dock.
- `design-v4/handwriting-compare.png` — NanumBarunPen beside Nanum Pen Script. The image is cropped on the right; column A is complete, B is partial.

## Where it applies

Handwriting = words a family member wrote or said. Everything the app says stays sans.

| Element | Selector (v4.1) | Now (sans) | Handwriting |
|---|---|---|---|
| Journal title on the page | `.ptitle` | 54 / 1.22, 700 | **72 / 1.12, 400** |
| Journal story on the page | `.pstory` | 28 / 1.78 | **38 / 1.42** |
| Photo caption text on the page | `.cap` (text only) | 18 | **25**, line-height 36 |
| Family quote on the page | `.pvoice q` | 26 / 1.5, 600 | **34 / 1.3, 400** |
| Compose: title field | `.field .v` and its `<input>` | 24, 700 | **34 / 1.2, 400** |
| Compose: story transcript and its textarea | `.heard` | 25 / 1.62 | **34 / 1.42** |
| Compose: family quote | `.heard.q` | 28 / 1.5, 600 | **38 / 1.35, 400** |
| Compose: caption fields in the strip | `.sitem .v` and its `<input>` | 20, 600 | **27 / 1.3, 400** |

Stays sans, unchanged:
- Top bar, tabs, buttons, chips, dock mode tiles, hints, status lines.
- All dates and numerals: day numerals, masthead month and weekday, counts (`사진 3장 · 음성 0:04`), timers.
- Caption index numbers `01 02 03` (`.cap b`).
- Labels: `가족의 한마디`, `제목`, `표지`, `사진 2`.
- Placeholders (`.ph`): `제목`, `말로 들려주면 이야기가 여기에 적혀요.` These are the app speaking.
- Album, favourites and calendar tiles, including the title and one-line story on the feature tile. Tiles are navigation.

Sizes are about 1.35× the sans sizes because this face draws small inside its em box. At those sizes the line heights come out nearly the same as today (story 54 px per line against 50), so nothing moves in the layout.

## Fit, checked in the sample

On the 620 px text column, with the sample journal:
- Title: one line at 68 px with about 15% to spare, so 72 px should also fit on one line. Keep the existing two-line clamp for longer titles.
- Story: three lines, same as today.
- Quote: one line inside the voice block at 34 px.
- Captions: both fit their rows.
- Dock: title field one line; `돌아가기 아쉬웠던 노을` fits the 197 px caption field at 27 px with no ellipsis.

Not checked: the compose canvas at 0.69 scale (story becomes about 26 px there, which should hold), and long user-typed text.

## CSS to add at the end of the stylesheet

It must come after the existing overrides near lines 278–308.

```css
@font-face{font-family:"PJ Hand";src:url(assets/fonts/NanumPenScript-Regular.ttf) format("truetype");font-weight:400;font-display:block}
:root{--hand:"PJ Hand",var(--sans)}

.ptitle,.pstory,.pvoice q,.cap,
.field .v,.field input,.heard,.heard textarea,.sitem .v,.sitem input{
  font-family:var(--hand);font-weight:400;letter-spacing:0;font-synthesis:none}

.ptitle{font-size:72px;line-height:1.12}
.pstory{font-size:38px;line-height:1.42}
.pvoice q{font-size:34px;line-height:1.3}
.cap{font-size:25px;line-height:36px}
.cap b{font-family:var(--sans);font-size:15px;font-weight:700}
.field .v,.field input{font-size:34px;line-height:1.2}
.heard,.heard textarea{font-size:34px;line-height:1.42}
.heard.q{font-size:38px;line-height:1.35}
.heard.q .soft{font-size:38px;font-weight:400}
.sitem .v,.sitem input{font-size:27px;line-height:1.3}

/* placeholders are the app speaking: keep sans at the old sizes */
.ptitle .ph{font-family:var(--sans);font-size:54px;font-weight:500}
.pstory .ph{font-family:var(--sans);font-size:28px}
.field .v.ph{font-family:var(--sans);font-size:24px;font-weight:500}
input::placeholder,textarea::placeholder{font-family:var(--sans)}
```

Selector names for the editable inputs are my best reading of v4.1; match them to the real input and textarea elements.

## Implementation notes

- Copy the font to `family-journal-v4.1/assets/fonts/NanumPenScript-Regular.ttf` and put `OFL-NanumPen.txt` beside it. Add one line to `ASSET-NOTES.md`: Nanum Pen Script, NAVER, SIL Open Font License 1.1.
- Never bold it. The family has one weight; `font-weight:400` plus `font-synthesis:none` stops the WebView from smearing a fake bold, which is what made NanumBarunPen look wrong in the comparison.
- Load before first paint of a journal: `document.fonts.load('38px "PJ Hand"')` before setting `demoReady`, so the recording never shows sans text swapping to handwriting.
- The file is 3.2 MB. That is fine for the APK; no subsetting needed today.
- The persimmon caret and the grey interim words during dictation keep their colours; only the face changes.

## What to look at in the first screenshots

1. Journal for the 15th: title on one line, voice block bottom still aligned with the bottom photo row.
2. Compose with dictation running: handwriting appearing word by word on the page and in the dock, no clipped descenders in the dock transcript.
3. Caption strip: third caption not cut off.
4. Album: no handwriting anywhere.
