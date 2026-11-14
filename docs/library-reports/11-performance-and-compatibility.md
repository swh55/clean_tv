# 11 — Performance, memory and the old TVs

**Report status:** PASS by construction and review; measured numbers on hardware
NOT TESTED (no device in this environment).

## Nothing new is heavy

- **Zero new dependencies.** The Library uses the platform's SQLite, MediaPlayer,
  DownloadManager, org.json and the launcher's own drawing — the APK grows only by
  its text assets (4.4 MB database, 33 KB of JSON), not by a single library.
- **The database is read-only after one copy** (a single 4.4 MB copy on first open,
  off the main thread, atomic via a `.new` file and a rename); every query is
  indexed and small — the worst a screen asks for is one surah's verses (286 rows,
  ~100 KB) on the worker lane.
- **One background lane** (`Lib`) carries every database and file touch, so the UI
  thread does layout only; the reader's position save writes at most one row a
  second; the player's position saver ticks once a second and writes every five.
- The reminder check adds a handful of integer comparisons to the existing
  15-second watch — nothing new wakes up, nothing polls the network.

## Playback memory

`Tilawa` is one MediaPlayer with the house backoff, exactly the radio's shape, and
the released player's `reset()` runs on its own thread the way the radio's does —
the launcher's own hard-learned rule for stuck streams. The visualizer the radio
carries is not duplicated. Downloads never cache in memory: they stream to files.

## Old TVs

minSdk stays 26, target 34, landscape-only, `configChanges` on every new screen so
no rebuild happens on the rotations a TV never does — the same manifest every
other screen ships. All text is drawn from the same token scaling (`Ui.px`), so
720p/1080p/4K all render the same design at their own density, as the rest of the
launcher does. RTL is driven by the saved language exactly as on Home. No new
permissions at all: the INTERNET, WAKE_LOCK and MODIFY_AUDIO the launcher already
had cover everything the Library asks.
