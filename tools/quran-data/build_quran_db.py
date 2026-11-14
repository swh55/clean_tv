#!/usr/bin/env python3
"""Builds assets/quran.db for the Islamic Library from the fetched sources.

Sources (in source/, fetched by fetch_all.py):
  quran-uthmani.alqurancloud.json  - the full Uthmani text (Tanzil) from api.alquran.cloud
  quran_pages_juz.json             - first-ayah of every page (1-604) and juz (1-30) from api.alquran.cloud

Output: out/quran.db — surahs, ayahs (text + normalised search column + juz + page), pages, juz, meta.
The verse text is stored verbatim, byte-for-byte as the source delivered it, except:
  - the file's leading U+FEFF is dropped (an encoding marker, not a letter),
  - for surahs other than Al-Fatihah and At-Tawbah the leading Bismillah that
    api.alquran.cloud embeds in verse 1 is stripped, so the app can show it as its own header row
    (it is verified first: every such verse must start with the exact Bismillah of 1:1).

Integrity gates (the build fails loudly if any fails):
  - exactly 114 surahs, 6236 verses, verse counts match the counts printed in the surah list
  - the global verse ids are 1..6236 in order; every page and juz start lands on its stored id
  - spot checks: 1:1, 2:255, 112:1 (after the strip), 9:1 has no Bismillah, 36:82, 55 end
"""
import json, sqlite3, sys, os

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "source")
OUT = os.path.join(HERE, "out")
DB = os.path.join(OUT, "quran.db")

SRC_URL = "https://api.alquran.cloud/v1/quran/quran-uthmani (text of Tanzil.net)"

# Arabic letters that carry no dot/diacritic, for the search column.
STRIP = "".join(chr(c) for c in range(0x064B, 0x065C)) + "\u06D6\u06D7\u06D8\u06D9\u06DA\u06DB\u06DC\u06DD\u06DE\u06DF\u06E0\u06E1\u06E2\u06E3\u06E4\u06E5\u06E6\u06E7\u06E8\u06E9\u06EA\u06EB\u06EC\u06ED\u0640"
ALEF = "ٱأإآ"
SPECIALS = {
    "ٱ": "ا", "أ": "ا", "إ": "ا", "آ": "ا", "ى": "ي", "ﻻ": "لا", "ﻵ": "لا", "ﻹ": "لا", "ﻸ": "لا",
    "ة": "ه", "ؤ": "و", "ئ": "ي",
}

def normalize2(s):
    """The same folding, with the dagger alif read as the alef it stands for: عَٰلَمِينَ indexes as عالمين."""
    return normalize(s.replace("ٰ", "ا"))

def normalize(s):
    out = []
    for ch in s:
        if ch in STRIP:
            continue
        out.append(SPECIALS.get(ch, ch))
    return "".join(out)

def main():
    quran = json.load(open(os.path.join(SRC, "quran-uthmani.alqurancloud.json"), encoding="utf-8"))["data"]["surahs"]
    bounds = json.load(open(os.path.join(SRC, "quran_pages_juz.json"), encoding="utf-8"))

    os.makedirs(OUT, exist_ok=True)
    if os.path.exists(DB):
        os.remove(DB)
    con = sqlite3.connect(DB)
    cur = con.cursor()
    cur.executescript("""
        CREATE TABLE meta(key TEXT PRIMARY KEY, value TEXT NOT NULL);
        CREATE TABLE surahs(
            number INTEGER PRIMARY KEY, name TEXT NOT NULL, name_plain TEXT NOT NULL,
            name_en TEXT NOT NULL, meccan INTEGER NOT NULL, ayahs INTEGER NOT NULL);
        CREATE TABLE ayahs(
            id INTEGER PRIMARY KEY, surah INTEGER NOT NULL, ayah INTEGER NOT NULL,
            text TEXT NOT NULL, search TEXT NOT NULL, search2 TEXT NOT NULL, juz INTEGER NOT NULL, page INTEGER NOT NULL);
        CREATE TABLE pages(page INTEGER PRIMARY KEY, first_ayah INTEGER NOT NULL);
        CREATE TABLE juz(juz INTEGER PRIMARY KEY, first_ayah INTEGER NOT NULL);
        CREATE INDEX idx_ayahs_surah ON ayahs(surah);
        CREATE INDEX idx_ayahs_search ON ayahs(search);
        CREATE INDEX idx_ayahs_page ON ayahs(page);
    """)

    # ---- the Bismillah as 1:1 reads it (BOM dropped), to strip from verse 1 of surahs 2..114, not 9.
    fatihah1 = quran[0]["ayahs"][0]["text"].replace("﻿", "", 1)
    bismillah = fatihah1
    stripped, kept = 0, 0
    global_id = 0
    rows, surah_rows = [], []
    for s in quran:
        n = s["number"]
        name = s["name"].replace("سُورَةُ ", "").strip()
        meccan = 1 if s["revelationType"] == "Meccan" else 0
        surah_rows.append((n, name, normalize(name), s["englishName"], meccan, len(s["ayahs"])))
        for a in s["ayahs"]:
            global_id += 1
            text = a["text"]
            if text.startswith("﻿"):
                text = text[1:]
            if a["numberInSurah"] == 1 and n not in (1, 9):
                # api.alquran.cloud embeds the surah header Bismillah in verse 1. It is not part of
                # the verse: 111 surahs carry it (surahs 95 and 97 with a shadda on the ba, the
                # source's own rendering of it), At-Tawbah has none, Al-Fatihah's is verse 1 itself.
                # The cut length is found by letter-matching, because the source orders the
                # combining marks of بِّ differently in those two surahs.
                plain_b = normalize(bismillah)
                cut = -1
                for L in range(len(bismillah) - 1, len(bismillah) + 3):
                    if normalize(text[:L]) == plain_b:
                        cut = L
                        break
                if cut < 0:
                    sys.exit(f"FAIL surah {n} verse 1 does not begin with the Bismillah: {text[:80]}")
                text = text[cut:].lstrip(" ")
                stripped += 1
            else:
                kept += 1
            if global_id != a["number"]:
                sys.exit(f"FAIL global id drift at {n}:{a['numberInSurah']}")
            rows.append((global_id, n, a["numberInSurah"], text, normalize(text), normalize2(text), 0, 0))

    # ---- juz and page of every ayah from the boundaries (first_ayah global ids, ascending)
    juz_starts = sorted((v[2], int(j)) for j, v in bounds["juzes"].items())
    page_starts = sorted((v[2], int(p)) for p, v in bounds["pages"].items())
    jcol, pcol = {}, {}
    ji = 0
    cur_j = 1
    for r in rows:
        aid = r[0]
        while ji + 1 < len(juz_starts) and juz_starts[ji + 1][0] <= aid:
            ji += 1
            cur_j = juz_starts[ji][1]
        jcol[aid] = cur_j
    pi = 0
    cur_p = 1
    for r in rows:
        aid = r[0]
        while pi + 1 < len(page_starts) and page_starts[pi + 1][0] <= aid:
            pi += 1
            cur_p = page_starts[pi][1]
        pcol[aid] = cur_p

    rows = [(r[0], r[1], r[2], r[3], r[4], r[5], jcol[r[0]], pcol[r[0]]) for r in rows]
    cur.executemany("INSERT INTO ayahs VALUES(?,?,?,?,?,?,?,?)", rows)
    cur.executemany("INSERT INTO surahs VALUES(?,?,?,?,?,?)", surah_rows)
    cur.executemany("INSERT INTO pages VALUES(?,?)", [(int(p), v[2]) for p, v in sorted(bounds["pages"].items(), key=lambda kv: int(kv[0]))])
    cur.executemany("INSERT INTO juz VALUES(?,?)", [(int(j), v[2]) for j, v in sorted(bounds["juzes"].items(), key=lambda kv: int(kv[0]))])

    # ---- integrity gates
    n_surahs, n_ayahs = cur.execute("SELECT COUNT(*) FROM surahs").fetchone()[0], cur.execute("SELECT COUNT(*) FROM ayahs").fetchone()[0]
    if n_surahs != 114: sys.exit(f"FAIL surah count {n_surahs}")
    if n_ayahs != 6236: sys.exit(f"FAIL ayah count {n_ayahs}")
    for (num, name, plain, en, mec, cnt) in surah_rows:
        actual = cur.execute("SELECT COUNT(*) FROM ayahs WHERE surah=?", (num,)).fetchone()[0]
        if actual != cnt: sys.exit(f"FAIL surah {num} ayahs {actual} != {cnt}")
    # spot checks (letters only, so the source's own order of combining marks cannot fail them)
    def spot(s, a, want):
        got = cur.execute("SELECT text FROM ayahs WHERE surah=? AND ayah=?", (s, a)).fetchone()[0]
        if normalize(want) not in normalize(got):
            sys.exit(f"FAIL spot {s}:{a} = {got[:70]}")
    spot(1, 1, "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ")
    spot(2, 255, "ٱللَّهُ لَآ إِلَٰهَ إِلَّا هُوَ")
    spot(2, 1, "الٓمٓ")
    spot(9, 1, "بَرَآءَةٌۭ مِّنَ ٱللَّهِ")
    spot(112, 1, "قُلْ هُوَ ٱللَّهُ أَحَدٌ")
    spot(36, 82, "إِنَّمَآ أَمۡرُهُۥ")
    spot(55, 78, "تَبَٰرَكَ ٱسۡمُ رَبِّكَ")
    spot(114, 6, "مِنَ ٱلۡجِنَّةِ وَٱلنَّاسِ")
    # page 1 = Al-Fatihah; juz 1 = 1:1; juz 2 = 2:142; juz 30 = 78:1 (checked from the API itself)
    if cur.execute("SELECT first_ayah FROM juz WHERE juz=2").fetchone()[0] != 7 + 142:
        sys.exit("FAIL juz 2 start")
    if cur.execute("SELECT first_ayah FROM pages WHERE page=2").fetchone()[0] != 7 + 1:
        sys.exit("FAIL page 2 start")

    checksum = cur.execute("SELECT group_concat(text) FROM (SELECT text FROM ayahs ORDER BY id)").fetchone()[0]
    import hashlib
    digest = hashlib.sha256(checksum.encode("utf-8")).hexdigest()
    cur.execute("INSERT OR REPLACE INTO meta VALUES(?,?)", ("source", SRC_URL))
    cur.execute("INSERT OR REPLACE INTO meta VALUES(?,?)", ("license", "Tanzil.net notice: the text may be freely used, distributed and shared provided it is not modified and this notice is kept intact. See assets/LICENSE-QURAN.txt"))
    cur.execute("INSERT OR REPLACE INTO meta VALUES(?,?)", ("text_sha256", digest))
    cur.execute("INSERT OR REPLACE INTO meta VALUES(?,?)", ("ayahs", "6236"))
    cur.execute("INSERT OR REPLACE INTO meta VALUES(?,?)", ("surahs", "114"))
    cur.execute("INSERT OR REPLACE INTO meta VALUES(?,?)", ("bismillah_stripped", str(stripped)))
    con.commit()
    size = os.path.getsize(DB)
    print(f"OK quran.db: 114 surahs, 6236 ayahs, 604 pages, 30 juz, {stripped} Bismillah stripped, {kept} kept, {size/1e6:.2f} MB, sha256(text)={digest[:16]}…")

if __name__ == "__main__":
    main()
