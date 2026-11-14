# 03 — Adhkar authenticity: the curation audit

**Report status:** PASS (the audit below is the build's own printed table; see
`azkar-audit.txt` beside these reports).

## Source

`Hisn al-Muslim` («حصن المسلم») by Sa'id ibn Ali ibn Wahf al-Qahtani — the chapters
«أذكار الصباح والمساء» (25 texts) and «الأذكار بعد السلام من الصلاة» (8 texts) — from
the book's digital text (github.com/rn0x/hisn_almuslim_json). The attribution shows in
the app (the Library home's header line) and in `assets/LICENSE-QURAN.txt`.

## The three rules, and how the build enforces them

1. **Verbatim text.** Every zikr shown is a string from the book's own text array —
   the script asserts each one exists in the source before it writes the asset. Where
   the book prints its evening variant after «وإذا أمسى قال…», the morning list shows
   the verbatim prefix and the evening list the verbatim whole text — nothing is
   reworded, joined or re-spelled.
2. **A repetition only where the book prints one.** Counts come from phrases inside the
   text or its footnote — ثلاث مرات, أربع مرات, سبع مرات, عشر مرات, مائة مرة, ثلاثاً,
   ثلاثاً وثلاثين — parsed longest-first, and every match prints in the audit. Entries
   without one count 1 (and «بعد كل صلاة مرة واحدة» counts 1, its threefold rule after
   Maghrib and Fajr left in the printed text itself). No count was invented anywhere.
3. **A source only where the pairing is certain.** The digital text's footnotes are a
   flat, sometimes shuffled list, so the curation attaches a footnote only where content
   matching is unambiguous (the canonical hadith for that exact zikr) or the numbered
   chain of Ibn As-Sunni's عمل اليوم والليلة confirms it (F11↔T11 via ابن السني 70,
   F15↔T15 via ابن السني 71, F30↔T20, F32↔T22). Where no footnote could be paired with
   certainty, the source line is the book itself — «حصن المسلم — الشيخ سعيد بن علي بن
   وهف القحطاني» — never a guessed hadith reference. Footnotes that could not be paired
   (F6, F8, F10, F27, F28) are dropped from display rather than risk a misattribution.

Quranic verses inside a zikr are the book's braced {…} pieces; the app colours them in
gold and sets them in Amiri — so the Qur'an and the hadith words are visually distinct,
and the distinction comes from the book's own marking, not a judgement of ours.

## The sections as shipped

- **Morning (23 zikrs):** the book's both-tagged and morning-tagged entries; the
  chapter's own opening hadith (عن أنس، أبو داود 3667) shows as the lead note.
- **Evening (20 zikrs):** the both-tagged and evening-tagged entries; the only
  evening-only zikr is «أعوذ بكلمات الله التامات» (three times, when evening comes —
  أبو داود 4/322).
- **After prayer (8 zikrs):** the whole chapter, footnotes aligned 1:1 by the source
  itself (استغفار ثلاثاً — مسلم 1/414; لا إله إلا الله وحده — البخاري ومسلم; سبحان
  الله والحمد لله والله أكبر ثلاثاً وثلاثين — مسلم 1/418 with the زبد البحر hadith;
  the المعوذات once after every prayer and three times after Maghrib and Fajr —
  أبو داود والنسائي; آية الكرسي عقب كل صلاة — النسائي في عمل اليوم والليلة 100;
  عشر مرات بعد المغرب والصبح — الترمذي 5/515; علماً نافعاً بعد الفجر — ابن ماجه).

The counts, tags and sources of all 51 entries print in `azkar-audit.txt`, one line
each, so the whole table is human-auditable after any rebuild.
