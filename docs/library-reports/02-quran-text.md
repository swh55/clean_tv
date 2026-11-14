# 02 — The Qur'an text: source, licence, integrity

**Report status:** PASS (all gates below ran against the exact asset shipped in the APK).

## Source

- The Uthmani text is **Tanzil's** (tanzil.net), fetched through `api.alquran.cloud`
  (`/v1/quran/quran-uthmani`) — the same text the Qur'an projects redistribute. The
  fetch is reproducible: `tools/quran-data/fetch_all.py` downloads it again, and
  `build_quran_db.py` rebuilds the database from it.
- **Licence:** Tanzil's notice ships inside the APK as `assets/LICENSE-QURAN.txt`, worded
  as Tanzil publishes it: the text may be freely used, distributed and shared provided
  it is not modified and the notice travels with it. The database's `meta` table also
  carries the notice and the source URL, so the attribution is queryable from the app.
- The page and juz boundaries came from the same source (each page 1–604 and juz 1–30
  endpoint), **cross-checked against quran.com's independent API** before the build
  accepted them (page 2 = 2:1, juz 2 = 2:142, juz 30 = 78:1).

## Integrity gates (the build fails loudly if any breaks)

`build_quran_db.py` verifies, every time it runs:

1. exactly **114 surahs** and **6236 verses**; every surah's verse count matches its row;
2. the global verse ids are 1..6236 in order — no drift, no gaps;
3. verse **1 of every surah except Al-Fatihah and At-Tawbah begins with the Bismillah**
   as the source embeds it (surahs 95 and 97 use the shadda-on-ba spelling the source
   itself prints — the cut length is found by letter-matching, not by a retyped string);
   the Bismillah is then **separated out** so the reader shows it as its own header row —
   the mushaf's own arrangement. At-Tawbah's first verse carries none, and the build
   checks it;
4. spot checks at 1:1, 2:1, 2:255, 9:1, 36:82, 55:78, 112:1, 114:6;
5. juz 2 starts at global id 149 (2:142), page 2 at 8 (2:1);
6. a SHA-256 of the whole verse text is written into the database's `meta` table:
   `20d583c4010f611037e6882d4257d304f4c9a348c279689ca3600123b5d2877b`.

## What is stored, and how search works

- `ayahs.text` is the verse **verbatim** — byte-for-byte as the source delivered it,
  with only two mechanical adjustments, both documented in the script and in the licence
  file: the file's leading U+FEFF marker is dropped, and the surah-header Bismillah is
  split out of verse 1 (112 first verses; Al-Fatihah's own Bismillah *is* verse 1 and
  stays).
- `ayahs.search` and `ayahs.search2` are letters-only foldings of the same text for
  search: diacritics, tatweel and Quranic annotation marks removed, ٱ/أ/إ/آ→ا, ى→ي,
  ة→ه, ؤ→و, ئ→ي. The two columns differ only in the **dagger alif**: the first drops it
  (mushaf spelling, matches typed «الله»), the second reads it as its letter ا
  (today's spelling, matches typed «العالمين»). A search matches either column —
  verified: «الله» finds 1746 verses through the first, «العالمين» finds 61 through
  the second (1:2, 2:47, 2:122 …). The app-side `Quran.normalize` is the same folding as
  the first column, and the equivalence is unit-tested.

## Offline

The whole database ships in the APK (`assets/quran.db`, 4.4 MB uncompressed) and is
copied once to the app's databases directory on first open (off the main thread);
after that, all reading and searching is fully offline.
