# 06 — Adhkar screens: counters, sessions, two modes

**Report status:** PASS (logic + persistence verified by code and unit tests; the
on-screen counting feel NOT TESTED without a TV).

## Categories and order

Three sections open from the Library home — morning, evening, after-prayer — each
showing the book's own order, with the count of its zikrs and today's progress in the
header («23 ذكرًا · 5/23»).

## Counting

- **OK on a zikr counts one repetition.** The card shows the book's own repetition
  (×3, ×7, ×10, ×33, ×100 — only where the book prints one; «مرة» where it does not),
  and under it the day's own tally «2 / 3». A zikr that reached its count shows
  «✓ أُتمّ الورد» and takes the gold ring; OK on it does nothing more — the counter
  never rolls over past the book's number.
- The session is **saved with every count** (`azkar_progress` rows per section, day
  and zikr) and shows again the next time — the same day. A new day starts fresh.
- **Reset** is a button, per section, from the screen's chips and from Library
  settings; it zeroes the counters of that section only.

## Two modes

- **قائمة (list):** the whole section scrolls; focus moves between zikrs, OK counts
  the focused one. The morning list opens with the chapter's own lead hadith as a
  quiet note — the book's introduction, not a zikr to count.
- **مفرد (one by one):** one zikr fills the screen in Amiri 40 with its repetition,
  a progress bar, a large counter, its source and its note; ◀▶ moves between zikrs,
  OK counts. The mode is remembered (Settings-stored) and switched from the chip.

## Sources and notes

Every zikr carries its source line verbatim (the hadith or the book itself — never a
guessed reference; the full audit is report 03), and the book's own evening variants
(«وإذا أمسى قال…») show as its note on the entries that have them — the reader always
sees where a wording came from. Quranic {…} pieces inside a zikr render in gold Amiri.

The after-prayer section keeps the book's own time rules inside the text it prints:
«بعد كل صلاة مرة واحدة، وبعد صلاة المغرب والفجر ثلاث مرات» counts 1, with the
threefold rule visible exactly as the book words it; «عشر مرات بعد صلاة المغرب
والصبح» counts 10 with its timing in the text.
