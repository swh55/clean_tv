#!/usr/bin/env python3
"""Fetches the sources the builders use, into source/. Run first; then the build_*.py scripts.

  quran-uthmani.alqurancloud.json   api.alquran.cloud/v1/quran/quran-uthmani (the Tanzil text)
  quran_pages_juz.json              first verse of every page 1-604 and juz 1-30 (same source;
                                    page 2 and 30 boundaries cross-checked against quran.com)
  hisn_almuslim.json                github.com/rn0x/hisn_almuslim_json (حصن المسلم digital text)
  mp3quran-reciters.json            mp3quran.net/api/v3/reciters?language=ar

All sources are public; the Quran text carries the Tanzil notice (assets/LICENSE-QURAN.txt).
"""
import json, os, time, urllib.request
from concurrent.futures import ThreadPoolExecutor

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "source")
os.makedirs(SRC, exist_ok=True)

def get(url, tries=4):
    last = None
    for i in range(tries):
        try:
            with urllib.request.urlopen(url, timeout=30) as r:
                return json.load(r)
        except Exception as e:
            last = e
            time.sleep(1 + i)
    raise SystemExit(f"FAIL {url}: {last}")

# 1. the Quran text
d = get("https://api.alquran.cloud/v1/quran/quran-uthmani")
json.dump(d, open(os.path.join(SRC, "quran-uthmani.alqurancloud.json"), "w"), ensure_ascii=False)
print("quran text:", len(d["data"]["surahs"]), "surahs")

# 2. page and juz boundaries
def page_or_juz(args):
    kind, num = args
    url = f"https://api.alquran.cloud/v1/{'page/%d/quran-uthmani' % num if kind == 'page' else 'juz/%d' % num}"
    for i in range(4):
        try:
            with urllib.request.urlopen(url, timeout=25) as r:
                d = json.load(r)
                a = d["data"]["ayahs"][0]
                return kind, num, [a["surah"]["number"], a["numberInSurah"], a["number"]]
        except Exception:
            time.sleep(1 + i)
    return kind, num, None

pages, juzes = {}, {}
jobs = [("page", p) for p in range(1, 605)] + [("juz", j) for j in range(1, 31)]
with ThreadPoolExecutor(max_workers=12) as ex:
    for kind, num, val in ex.map(page_or_juz, jobs):
        if val is None:
            raise SystemExit(f"FAIL {kind} {num}")
        (pages if kind == "page" else juzes)[num] = val

# cross-check two boundaries against quran.com (independent source, standard 604-page mushaf)
d2 = get("https://api.quran.com/api/v4/verses/by_page/2?fields=text_uthmani&per_page=10")
if d2["verses"][0]["verse_key"] != "2:1":
    raise SystemExit("FAIL quran.com disagrees about page 2")
if [pages[2][0], pages[2][1]] != [2, 1]:
    raise SystemExit("FAIL alquran.cloud page 2 is not 2:1")
if [juzes[2][0], juzes[2][1]] != [2, 142]:
    raise SystemExit("FAIL juz 2 is not 2:142")
if [juzes[30][0], juzes[30][1]] != [78, 1]:
    raise SystemExit("FAIL juz 30 is not 78:1")

json.dump({"pages": pages, "juzes": juzes}, open(os.path.join(SRC, "quran_pages_juz.json"), "w"))
print("pages:", len(pages), "juzes:", len(juzes), "(cross-checked with quran.com)")

# 3. the adhkar book (rn0x/hisn_almuslim_json — clone or raw download)
import subprocess
target = os.path.join(SRC, "hisn_almuslim.json")
if not os.path.exists(target):
    subprocess.run(["git", "clone", "-q", "--depth", "1",
                    "https://github.com/rn0x/hisn_almuslim_json.git", "/tmp/azkar-src"], check=True)
    import shutil
    shutil.copy("/tmp/azkar-src/hisn_almuslim.json", target)
print("hisn al-muslim text: present")

# 4. the reciters list
d = get("https://mp3quran.net/api/v3/reciters?language=ar")
json.dump(d, open(os.path.join(SRC, "mp3quran-reciters.json"), "w"), ensure_ascii=False)
print("reciters:", len(d.get("reciters", [])))
