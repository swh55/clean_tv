# 07 — Reading the Qur'an: index, search, reader, bookmarks, positions

**Report status:** PASS (data and logic verified; on-screen reading NOT TESTED).

## The index

All 114 surahs on a six-column grid: number in gold, name in Amiri, «مكية/مدنية ·
N آية». Search opens a panel with the remote's own keys:

- **بالرقم:** a digit pad (0–9, ⌫) — typing 2 finds Al-Baqarah, 112 finds Al-Ikhlas;
  OK opens the match.
- **بالاسم:** a letter grid (Arabic 28 letters for Arabic/Urdu, A–Z for the Latin
  languages) building a prefix — the matches list below narrows live through the
  same letters-only folding the search columns use; OK opens the first match.

A «أكمل القراءة» chip sits in the header once any reading position exists; it opens
the reader straight at the saved verse.

## The reader

- Each verse is its own focusable row: the Uthmani text in Amiri with the verse
  number in the mushaf's ornate parentheses «﴿٥٧﴾» as a gold span — one TextView,
  nothing retyped. The Bismillah heads every surah but Al-Fatihah (where it is verse
  one) and At-Tawbah (which has none) — spelled exactly as the database carries it,
  read from 1:1, never a constant string.
- The header names the surah and its shape («مكية · 286 آية · الجزء 1 · الصفحة 2»)
  and a live line under it tells where the focused verse sits («الجزء 3 · الصفحة
  42»), taken from the verse's own row.
- **Text size:** the A-chip cycles five sizes (30–50 design px), saved — and the
  Library settings carry it too.
- **انتقال إلى آية:** a digit pad builds a verse number; the reader scrolls to the
  verse and focuses it.
- **استماع:** opens the player on this surah; **العلامات** opens the bookmark list.
- **OK on a verse toggles its bookmark** — the verse row is the whole interaction,
  marked by a quiet ✓ note; bookmarks are strictly optional, never needed for any
  other feature.
- **Verse search** (in the index's panel, by name) is covered by the two-column
  search of report 02: queries typed with today's spelling find the mushaf's dagger
  spellings and the classic ones alike.

## Positions, kept honest

- The **reading position** saves the focused verse's own surah, verse, page and juz —
  written at most once a second while reading (the reader's tick), and on Back,
  pause and navigation. Continue reading (the Library home's hero line, the continue
  card, the index chip) all return to that verse.
- The **audio position** is a separate table keyed by reciter and chapter — listening
  never moves the reading position, and vice versa (report 08).
- Both live in `library.db`; the day's first read of Al-Fatihah verse one is not
  mistaken for "no position" (a row exists or it does not — `hasReadPos`).
