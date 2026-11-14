# 04 — Storage: what lives where, and nothing heavy on the UI thread

**Report status:** PASS (design as shipped; threading verified by code review — on-device
timing NOT TESTED).

## Two databases, two jobs

1. **`quran.db`** (shipped in the APK, read-only after one copy): the Qur'an text,
   the page and juz boundaries, the search columns, the meta table with its checksum.
   It is the static content — never written after the first copy, recopied only when a
   new APK carries a newer `DB_VERSION` (bumped when the asset changes).
2. **`library.db`** (the app's own SQLite, created on first use): everything the user
   does — the reading position, the audio position per reciter and chapter, bookmarks,
   the khatm plans and their day-by-day log, the download registry, the adhkar counters.
   Small rows, all indexed, nothing grows without the user asking for it.

SharedPreferences keeps only what it is for: switches, times, the chosen reciter, the
text size, the adhkar view mode, and the reminder "already shown" stamps. **No verse
text and no logs ever sit in SharedPreferences** — exactly the rule the brief set.

## Threading

- The first open (and the 4.4 MB asset copy) runs on the `quran-open` thread;
  `Quran.ready()` hands control back to the main thread when it is done, and screens
  build their content then — never blocking.
- `UserDb` is reached only through `Lib.io`, one background lane for the whole Library;
  results come back through a main-thread post. The one-second reader tick that saves
  the position writes at most one small row a second, off the main thread.
- The surah table (114 rows) is cached in memory; a surah's verses load on the worker
  thread and are handed back whole — Al-Baqarah's 286 verses are the worst case.

## Sizes

- APK grows by the assets: `quran.db` 4.4 MB (uncompressed in the APK), `azkar.json`
  30 KB, `reciters.json` 2 KB, the licence file 1.5 KB. The debug APK weighed
  2.25–2.9 MB against 777 KB before the assets — the launcher's APK size gate
  (the 557 KB release gate of the old clean_tv build) does not apply to this project's
  releases, which already ship ~20 MB of adhan audio.
- Downloads live under the app's own external files (`files/quran/<reciter>/<NNN>.mp3`),
  so uninstall clears them and nothing ever writes to shared storage.
