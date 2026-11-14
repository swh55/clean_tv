# 12 — QA: what was tested, what was not

**Report status:** this is the test report itself. No Android TV hardware exists in
this environment, so every on-device behaviour is labelled NOT TESTED — nothing is
claimed that was not run.

## Automated — PASS

| Check | Result |
|-------|--------|
| `./gradlew assembleDebug` (JDK 17, SDK 34) | **PASS** — BUILD SUCCESSFUL |
| APK badging (`aapt dump badging`) | **PASS** — `pro.datascoop.tv`, versionCode 12, versionName 0.9, leanback-launchable MainActivity |
| Assets present in the APK | **PASS** — `quran.db` (4,431,872 B), `azkar.json` (30,541 B), `reciters.json` (2,178 B), `LICENSE-QURAN.txt` |
| `./gradlew testReleaseUnitTest` | **PASS** — 37 tests, 0 failures (PrayerTimes 5, Weather 6, Text 8, Tokens 4, **Library 14**) |
| Library unit tests | **PASS** — letters-only folding (including the ٱ→ا and ة→ه folds and the two-column search contract), khatm day arithmetic across months and years, the adhkar count words in five languages, the reminder words per kind, juz/page lines, byte sizes |
| Existing suites after integration | **PASS** — untouched behaviour: prayer times, weather, interface strings, tokens all still green |

## Data pipeline — PASS (scripted gates, every build)

114 surahs, 6236 verses, per-surah counts, ordered global ids, 604 pages and 30 juz
boundaries cross-checked against quran.com, the Bismillah separation gate (112
first verses, At-Tawbah without one), spot checks at eight verses, SHA-256 recorded
in the database; azkar verbatim assertions and the printed audit table; reciter
URLs answered HTTP 200. Search behaviour verified directly on the shipped database:
«الله» → 1746 verses, «العالمين» → 61 verses (1:2, 2:47…).

## Static review — PASS by review, runtime NOT TESTED

- Integration points (Home's pill and focus map, the Settings section, the watch
  tick, the radio/recitation mutual stop, the manifest's ten activities and one
  receiver) were re-read against the existing code paths.
- Java compile warnings are the two pre-existing deprecated-API notes the project
  already carried.

## NOT TESTED — no TV in this environment

- The screens' rendered look and D-pad focus travel (bento grid, reader scrolling,
  the adhkar counter's feel, the player's seek bar by focus).
- Audio: actual playback, the adhan's focus-loss pause/resume of a recitation, the
  cross-screen carry-on, the preview, the retry path on a broken stream.
- DownloadManager on a TV: enqueue, progress, the finisher broadcast, the file
  hand-off to the player.
- Reminders as seen: the Home card, the banner over another app, the screensaver
  and call guards in the living room.
- RTL mirroring of every screen on a real panel, the five languages' layouts at
  720p/1080p/4K, first-open latency of the 4.4 MB database copy on an old box.

These are the honest bounds: everything that could run here ran, and everything that
needs a TV is named, not claimed.
