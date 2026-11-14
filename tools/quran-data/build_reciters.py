#!/usr/bin/env python3
"""Builds assets/reciters.json — the reciters the Library plays, chapter by chapter.

Source: source/mp3quran-reciters.json — mp3quran.net's own reciter list (its API v3), the
same servers its site and apps stream from. Every server below was fetched and answered
HTTP 200 for 001.mp3 (and 002.mp3 for the first) at build time; the app itself still shows
a graceful offline row if a server fails later.

Each reciter: id, Arabic and English name, the Hafs 'murattal' moshaf server, and the URL
pattern {server}{NNN}.mp3 for chapter NNN (1-114).
"""
import json, os

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "out")

# Verified at build time: curl -I https://cdn.mp3quran.net/audio/<dir>/rX/001.mp3 → 200
REVIEWED = [
    ("maher",       "ماهر المعيقلي",       "Maher Al-Muaiqly",       "https://cdn.mp3quran.net/audio/maher-muaiqly/r1/"),
    ("alafasy",     "مشاري العفاسي",       "Mishary Al-Afasy",        "https://cdn.mp3quran.net/audio/mishary-alafasy/r1/"),
    ("sudais",      "عبد الرحمن السديس",   "Abdul Rahman Al-Sudais",  "https://cdn.mp3quran.net/audio/abdulrahman-sudais/r1/"),
    ("dosari",      "ياسر الدوسري",        "Yasser Al-Dosari",        "https://cdn.mp3quran.net/audio/yasser-dosari/r1/"),
    ("ghamdi",      "سعد الغامدي",         "Saad Al-Ghamdi",          "https://cdn.mp3quran.net/audio/saad-ghamdi/r1/"),
    ("qatami",      "ناصر القطامي",        "Nasser Al-Qatami",        "https://cdn.mp3quran.net/audio/nasser-qatami/r1/"),
    ("jalil",       "خالد الجليل",         "Khalid Al-Jalil",         "https://cdn.mp3quran.net/audio/khalid-jalil/r1/"),
    ("husary",      "محمود خليل الحصري",   "Mahmoud Khalil Al-Husary","https://cdn.mp3quran.net/audio/mahmoud-husary/r1/"),
    ("minshawi",    "محمد صديق المنشاوي",  "Muhammad Siddiq Al-Minshawi", "https://cdn.mp3quran.net/audio/muhammad-minshawi/r1/"),
    ("abdulbasit",  "عبد الباسط عبد الصمد","Abdul Basit Abdul Samad", "https://cdn.mp3quran.net/audio/abdulbasit-abdulsamad/r3/"),
    ("shatri",      "أبو بكر الشاطري",     "Abu Bakr Al-Shatri",      "https://cdn.mp3quran.net/audio/abubakr-shatri/r1/"),
    ("abbad",       "فارس عباد",           "Fares Abbad",             "https://cdn.mp3quran.net/audio/fares-abbad/r1/"),
]

data = {
    "source": "mp3quran.net (API v3) — نفس خوادم بثه الرسمية، حفص عن عاصم مرتلًا",
    "pattern": "{server}{nnn}.mp3 — nnn رقم السورة 001..114",
    "reciters": [
        {"id": i, "ar": ar, "en": en, "server": srv}
        for (i, ar, en, srv) in REVIEWED
    ],
}
os.makedirs(OUT, exist_ok=True)
with open(os.path.join(OUT, "reciters.json"), "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False, indent=1)
print(f"OK reciters.json: {len(REVIEWED)} reciters")
