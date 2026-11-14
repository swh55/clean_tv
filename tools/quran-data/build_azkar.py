#!/usr/bin/env python3
"""Builds assets/azkar.json — morning, evening and after-prayer adhkar of Hisn al-Muslim.

Source: source/hisn_almuslim.json — the digital text of «حصن المسلم» by Sa'id ibn Ali ibn
Wahf al-Qahtani (github.com/rn0x/hisn_almuslim_json), chapter «أذكار الصباح والمساء»
(25 texts) and «الأذكار بعد السلام من الصلاة» (8 texts, footnotes aligned 1:1).

Rules this build follows (the project's adhkar-reliability rules):
  - TEXT is verbatim from the book's text array. Nothing is reworded, joined or re-spelled.
    For texts that carry their own evening variant after «وإذا أمسى...», the morning list shows
    the verbatim prefix (the zikr itself) and the evening list the whole text.
  - COUNT comes only from a repetition the book itself prints inside the text or the footnote
    (ثلاث مرات، أربع مرات، سبع مرات، عشر مرات، مائة مرة، ثلاثاً، ثلاثاً وثلاثين). No count
    is invented: entries without one count 1.
  - SOURCE is a verbatim footnote of the same book, attached only where the pairing is certain
    (content-matched, or the numbered chain of Ibn As-Sunni's عمل اليوم والليلة confirms it).
    Where no footnote can be paired with certainty, the source is the book itself — never a
    guessed hadith reference.
  - TAGS (morning/evening) come from the book's own wording: أصبح/يصبح → morning,
    أمسى/يمسي/أمسينا → evening, both mentioned → both; the chapter's unmarked zikr are both.

The audit table below prints one line per entry (text head · count · tags · source head) and
the script refuses to build unless every index, prefix and footnote it uses exists verbatim.
"""
import json, os, sys, re

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "source", "hisn_almuslim.json")
OUT = os.path.join(HERE, "out")

src = json.load(open(SRC, encoding="utf-8"))
ME = src["أذكار الصباح والمساء"]
PP = src["الأذكار بعد السلام من الصلاة"]
T, F = ME["text"], ME["footnote"]
PT, PF = PP["text"], PP["footnote"]

def fn(i):
    """Footnote i, verbatim; must exist."""
    if i is None: return ""
    if i >= len(F): sys.exit(f"FAIL footnote {i} out of range")
    return F[i].strip()

def pfn(i):
    if i >= len(PF): sys.exit(f"FAIL post-prayer footnote {i} out of range")
    return PF[i].strip()

COUNTS = {"ثلاثاً وثلاثين": 33, "مائة مرة": 100, "مائة مرة)": 100, "مئة مرة": 100,
          "أربع مرات": 4, "سبع مرات": 7, "عشر مرات": 10, "عشر مرات)": 10,
          "ثلاث مرات": 3, "ثلاثاً": 3}

def count_of(text, source):
    """A repetition the book itself prints, parsed only from its own words (longest first)."""
    for pat, n in COUNTS.items():
        if pat in text:
            # «بعد كل صلاة مرة واحدة» reads once; the threefold rule after Maghrib and Fajr
            # stays in the printed text itself.
            if pat == "ثلاث مرات" and "مرة واحدة" in text and text.find("مرة واحدة") < text.find(pat):
                return 1, "مرة واحدة"
            return n, pat
    for pat, n in COUNTS.items():
        if pat in source:
            return n, pat
    return 1, ""

def split_evening(text):
    """The book's own evening variant sits after وإن أمسى/وإذا أمسي: morning gets the prefix."""
    m = re.search(r"وإذا أمس[يى]", text)
    return (text[:m.start()].strip(), text) if m else (text, text)

def entry(idx, tags, note_idx=None, source_idx=None, extra_note=None, count_override=None):
    text = T[idx].strip()
    morning, evening = split_evening(text)
    source = fn(source_idx) if source_idx is not None else ""
    note = fn(note_idx) if note_idx is not None else ""
    if extra_note:
        note = (note + "\n" + extra_note).strip()
    n, phrase = count_override if count_override else count_of(text, source)
    if not source:
        source = "حصن المسلم — الشيخ سعيد بن علي بن وهف القحطاني"
    return {
        "text": evening, "textMorning": morning if morning != evening else None,
        "count": n, "countPhrase": phrase,
        "tags": tags, "note": note, "source": source,
        "_t": text[:38],
    }

# ---- the audited table: morning/evening chapter. F indexes are certain pairings only.
# both-tagged zikr appear in the two lists; morning-only/evening-only only in their own.
ENTRIES = [
    entry(1,  ["m", "e"], source_idx=1),                       # آية الكرسي — F1 البقرة 255 + الحاكم
    entry(2,  ["m", "e"], source_idx=2),                       # المعوذات ×3 — F2 حين يصبح وحين يمسي
    entry(3,  ["m", "e"], note_idx=4),                         # أصبحنا وأصبح الملك لله — F4 خير الليلة
    entry(4,  ["m", "e"]),                                     # اللهم بك أصبحنا (المساء في المتن نفسه)
    entry(5,  ["m", "e"], source_idx=5),                       # سيد الاستغفار — مسلم 4/2088
    entry(6,  ["m"]),                                          # اللهم إني أصبحت أشهدك ×4
    entry(7,  ["m", "e"], source_idx=7, note_idx=12),          # اللهم ما أصبح بي — الترمذي + F12 أمسى
    entry(8,  ["m", "e"]),                                     # اللهم عافني ×3
    entry(9,  ["m", "e"], source_idx=9),                       # حسبي الله ×7 — البخاري 7/150
    entry(10, ["m", "e"]),                                     # العفو والعافية
    entry(11, ["m", "e"], source_idx=11),                      # عالم الغيب ×4 — F11 أربع مرات
    entry(12, ["m", "e"], source_idx=18),                      # بسم الله الذي لا يضر ×3 — F18
    entry(13, ["m", "e"], source_idx=13, extra_note=fn(19)),   # رضيت بالله ×3 — F13 + F19
    entry(14, ["m", "e"], source_idx=14),                      # يا حي يا قيوم — أبو داود 4/324
    entry(15, ["m", "e"], source_idx=15, note_idx=21, extra_note=fn(22)),  # خير هذا اليوم ×7 — F15، الف21/22
    entry(16, ["m", "e"], source_idx=16, extra_note=fn(24)),   # فطرة الإسلام — F16 + F25 + F24
    entry(17, ["m", "e"], source_idx=17, extra_note=fn(26)),   # سبحان الله وبحمده ×100 — F17 + F26
    entry(18, ["m", "e"]),                                     # لا إله إلا الله ×10 أو مرة عند الكسل
    entry(19, ["m"],       source_idx=29),                     # ×100 إذا أصبح — البخاري 4/95 مسلم 4/2071
    entry(20, ["m"],       source_idx=30),                     # عدد خلقه ×3 إذا أصبح — مسلم 4/2090
    entry(21, ["m"],       source_idx=31),                     # علماً نافعاً إذا أصبح — ابن السني 54
    entry(22, ["m", "e"],  source_idx=32),                      # أستغفر الله ×100 — البخاري ومسلم
    entry(23, ["e"],       source_idx=23, extra_note=fn(33)),  # أعوذ بكلمات ×3 إذا أمسى — F23 + F33
    entry(24, ["m", "e"],  source_idx=34),                      # الصلاة على النبي ×10 — الطبراني F34
]
INTRO = fn(0)  # عن أنس — الفضل — the chapter's own opening hadith, shown as the morning lead

# ---- after-prayer chapter: texts and footnotes align 1:1
PP_ENTRIES = []
for i, text in enumerate(PT):
    text = text.strip()
    n, phrase = count_of(text, pfn(i))
    PP_ENTRIES.append({
        "text": text, "count": n, "countPhrase": phrase,
        "tags": ["p"], "note": "", "source": pfn(i), "_t": text[:38],
    })

# ---- audit print (goes into the build report)
print("=== أذكار الصباح والمساء (morning/evening) ===")
for e in ENTRIES:
    tags = "+".join(e["tags"])
    print(f"[{e['count']:>3}x {tags:5s}] {e['_t']}")
    print(f"           المصدر: {e['source'][:72]}")
    if e["note"]: print(f"           ملاحظة: {e['note'][:72]}")
print("=== أذكار بعد الصلاة (after prayer) ===")
for e in PP_ENTRIES:
    print(f"[{e['count']:>3}x] {e['_t']}")
    print(f"           المصدر: {e['source'][:72]}")

# ---- verify verbatim: every text and every attached footnote exists in the source arrays
for e in ENTRIES:
    assert e["text"] in " ".join(T), e["_t"]
    if e["textMorning"]:
        assert any(t.strip().startswith(e["textMorning"]) or e["textMorning"] in t for t in T), e["_t"]
for e in PP_ENTRIES:
    assert e["text"] in " ".join(PT)

# ---- split into the three lists
def has(e, tag): return tag in e["tags"]
def clean(e, which):
    out = {"text": e.get("textMorning") if which == "m" and e.get("textMorning") else e["text"],
           "count": e["count"], "source": e["source"], "note": e["note"]}
    return out

morning = [clean(e, "m") for e in ENTRIES if has(e, "m")]
evening = [clean(e, "e") for e in ENTRIES if has(e, "e")]
after = [{"text": e["text"], "count": e["count"], "source": e["source"], "note": e["note"]} for e in PP_ENTRIES]

data = {
    "source": "حصن المسلم — الشيخ سعيد بن علي بن وهف القحطاني. النص الرقمي: github.com/rn0x/hisn_almuslim_json (فصلا أذكار الصباح والمساء والأذكار بعد السلام من الصلاة، حرفيًا).",
    "intro": INTRO,
    "morning": morning, "evening": evening, "afterPrayer": after,
}

os.makedirs(OUT, exist_ok=True)
with open(os.path.join(OUT, "azkar.json"), "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False, indent=1)
print(f"\nOK azkar.json: morning {len(morning)}, evening {len(evening)}, afterPrayer {len(after)}")
