# 01 — Brief and plan: adding the Islamic Library to Scoop TV

**Feature:** the Islamic Library (المكتبة الإسلامية) — an integrated section of the existing
Afnon-lineage launcher (Scoop TV, `pro.datascoop.tv`), designed as a Bento Box grid.
**Report status:** PASS (the plan was executed in full; every item's own report follows).

## What was inspected first

The whole codebase was read before a line was written, exactly as the brief demanded:

- `Screen` (the base every screen builds on: `build()`/`onTick()`, design-px placement,
  fonts, RTL mirroring, the once-a-second tick), `MainActivity` (Home's focus map and its
  card patterns), `SettingsActivity` (its Row framework: VALUE/SEG/STEP/TOGGLE),
  `AllAppsActivity`/`PrayerDayActivity` (list and paging patterns), `Radio` (the one
  streaming player, its audio-focus rules, its sleep-timer and Wi-Fi-lock arrangement),
  `PrayerWatch`/`Reminders` (the 15-second watch and the quiet banner over other apps),
  `Adhans`, `Tokens`, `Icon`, `Prefs`, `Text`.
- Constraints carried over untouched: Java only, no AndroidX, Android Views only, no new
  heavy libraries (the Library adds **zero** dependencies — SQLite, DownloadManager,
  MediaPlayer and org.json are all in the platform), no CDNs, offline-first text.

## The plan, as executed

| # | Planned | Delivered | Report |
|---|---------|-----------|--------|
| 1 | Adhkar of morning, evening and after-prayer: categories, ordered, counted, two modes, saved sessions, reset, sources | `AdhkarActivity` over the curated `azkar.json` | 03, 06 |
| 2 | Qur'an library: 114 surahs, search by name and number, continue reading, reader with Tanzil text, juz/page, jump, verse search | `QuranIndexActivity` + `QuranReaderActivity` + bundled `quran.db` | 02, 07 |
| 3 | Reciter choice (cards, info, preview, default saved) and player (all controls, focus-safe, across screens) | `RecitersActivity` + `Tilawa` + `PlayerActivity` | 08 |
| 4 | Reading position persisted (text separate from audio), continue cards, optional bookmarks | `UserDb` read/audio position tables + `BookmarksActivity` | 07, 10 |
| 5 | Optional daily reminders (5 kinds, off by default, no interruption, no sound, elegant card) | `LibReminders` + Home card + banner | 09 |
| 6 | Khatm plans: presets and custom, progress, manual day marking, delay redistribution, multiple plans | `KhatmActivity` + `UserDb` plan tables | 10 |
| 7 | Library home with six main cards + status cards, Bento Box | `LibraryActivity` | 05 |

Nothing of the existing launcher was rewritten: the integration into existing code is
four small touches — the Library pill in Home's header (with its focus-map entry), the
ninth Settings section, one guard in `Radio.start` (the two sounds are one source), and
one tick in `PrayerWatch.check` (the Library's reminders ride the existing watch).

## Where the code lives

New files (12 Java + assets + pipeline):
`LibraryActivity`, `AdhkarActivity`, `QuranIndexActivity`, `QuranReaderActivity`,
`BookmarksActivity`, `RecitersActivity`, `PlayerActivity`, `DownloadsActivity`,
`KhatmActivity`, `LibrarySettingsActivity`, `Tilawa`, `LibReminders`, `Quran`, `Azkar`,
`Reciters`, `UserDb`, `Downloads`, `Lib` — and `tools/quran-data/` (fetch + build scripts
and the sources they fetched). Touched: `MainActivity`, `SettingsActivity`,
`PrayerWatch`, `Radio`, `Prefs`, `Text`, `Icon`, `AndroidManifest.xml`.
