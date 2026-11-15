# Pleos Journal v4 — what the references taught

Seven Mobbin screenshots were supplied by the parent agent in `references/` (raw metadata with the original tracked URLs: `references/metadata.json`). This file replaces the parent's shorter `references.md` list: on this case-insensitive disk `REFERENCES.md` and `references.md` are the same file. I opened and viewed all seven. All are iOS portrait phone screens; Pleos Journal is Android landscape at 1920×1080, so every pattern below was re-laid-out rather than transplanted.

No app named Journal Labs was found on Mobbin, so nothing here is attributed to it. The "Journal" reference is Apple's Journal for iOS.

| File | App | Source |
|---|---|---|
| `5-minute-gallery.webp` | 5 Minute Journal | https://mobbin.com/screens/32ac43ac-3fe9-472a-ad81-75686b6ad078 |
| `5-minute-entry.webp` | 5 Minute Journal | https://mobbin.com/screens/4aad9872-8d9c-42e6-aead-10674eb45aef |
| `journal-mixed-media.webp` | Journal | https://mobbin.com/screens/85439bb8-1bea-438b-b0ce-72e1f706b9d6 |
| `corner-photo-history.webp` | corner | https://mobbin.com/screens/2fcd0d19-8482-4120-b7d7-66387e609be3 |
| `how-we-feel-attachments.webp` | How We Feel | https://mobbin.com/flows/e0cb8a58-4437-49df-9827-732a5a0ace87?tab=screens |
| `how-we-feel-recording.webp` | How We Feel | same flow |
| `stoic-journal-history.webp` | stoic. | https://mobbin.com/flows/32293b38-33d3-48ab-bced-fab3f1a7b686?tab=screens |

## What I saw, and what it became

### 5 Minute Journal — gallery
**Seen:** square photo tiles with the day number and month set large in white directly on the photo (`29 SEP`), a duration chip (`0:22`) in the tile corner, and no label box under the photo.
**Used:** album tiles are full-bleed photos with a large day numeral, month and weekday over a bottom scrim, and chips for photo count and voice length. This replaces v3's half-photo, half-white-box cards.
**Not used:** the square crop. The sample photos are 3:2 family group shots; square would cut people off, so tiles stay near 3:2. Also not used: the list/grid toggle, the warm grey background, the serif heading.

### 5 Minute Journal — entry list
**Seen:** a small date block (`TUE 29`) beside a one-line prompt, then one very large photo taking most of the screen.
**Used:** the idea that one photo should dominate. The newest journal is a 2×2 feature tile in the album, and the journal page gives the cover photo more area than the other two combined.

### Journal (iOS) — mixed-media entry
**Seen:** an entry built as a mosaic where a photo, a voice clip (play button, waveform, `0:09`) and other blocks are tiles of equal standing inside one card, with the title and text underneath and the date as a footer.
**Used:** the voice note is part of the journal page, set in the same column as the story with its own play button, waveform and length, instead of a separate attachment card. The page is one composed object.
**Not used:** the white rounded card stack, the purple accent, mood and location tiles (no such data in this product).

### corner — photo history
**Seen:** years as a row of very large bold text tabs (`2025 2024 2023`), the inactive ones grey; month names as plain headings; dense photo grid with 2 px gaps and almost no other chrome.
**Used:** month tabs in the library header are large type, not pills, with the year set thin beside them. Navigation in general is text-weight based.
**Not used:** per-month sections. The sample library has about one journal a month, and month sections would each hold a single tile. One continuous mosaic with the date on each tile works at five journals and at fifty.

### How We Feel — attachments and recording
**Seen:** on a black screen, `Add Photo` and `Add Voice` are equal tiles side by side. Once used, the photo tile shows the photo and the voice tile shows `Voice Recording 6s`. While recording, a waveform fills from the left in yellow with a timer, above a large round stop button; everything else dims.
**Used:**
- Compose has three peer mode tiles (사진 · 이야기 · 한마디) that show their own state, and the photo tile takes the chosen cover as its background.
- Recording UI is a large round button with the waveform and timer directly beside it.
- The decision to use a dark canvas, where photos and a single warm signal colour carry the screen.

**Not used:** the yellow accent (Pleos Journal uses persimmon, and only for today, recording and favourite), the serif display type, the tall pill-shaped tiles, health data tiles.

### stoic. — journal history
**Seen:** `Day Week Month Year` as plain text tabs with an underline; a reading card with a small spaced label, bold title, time and a voice glyph at top right, then generously spaced body text; controls kept outside the reading area.
**Used:** reading controls live in the top bar on the dark canvas, never on the paper page. Story text gets long line height (1.76) and nothing competes with it. A voice glyph with length sits in the page masthead counts.
**Not used:** the bottom tab bar with a centre plus button (wasteful in landscape; the top bar holds navigation and 새 일기), the monochrome palette.

## What is Pleos Journal's own

None of the references has these, and they are the core of the direction:
- A dark app surface with a single paper-coloured page as the journal, shared between composing and reading.
- A live page canvas beside the input dock, so the journal visibly assembles while the user picks photos and speaks.
- A persistent Today panel on the album with the day's photos waiting as a stack of prints.
- The day numeral as the one recurring graphic element across album, compose and journal.
