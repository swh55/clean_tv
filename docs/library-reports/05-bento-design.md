# 05 — The Bento Box design, in the layout itself

**Report status:** PASS (the grid is in the code, sized by importance; on-screen look
NOT TESTED without a TV).

The brief demanded Bento Box as a real layout, not a report's vocabulary word. The
Library home (`LibraryActivity`) is a bento grid on the launcher's own 1920×1080
design canvas — every rectangle from its constants:

| Card | Size (design px) | Position | Weight |
|------|------------------|----------|--------|
| **المصحف** (the Qur'an) | 560 × 742 | start column, full height | the tall hero: two rows' height plus the status row's |
| **أذكار الصباح** | 365 × 270 | row 1, cell 1 | medium |
| **أذكار المساء** | 365 × 270 | row 1, cell 2 | medium |
| **أذكار بعد الصلاة** | 365 × 270 | row 1, cell 3 | medium |
| **أكمل القراءة** | 365 × 270 | row 2, cell 1 | medium |
| **ختمة القرآن** | 754 × 270 | row 2, spanning two cells | the wide card |
| status cards | 209 × 154 | row 3, up to five | small, only when they have something to say |

That is the bento grammar the brief named: one tall hero, three mediums, one wide
spanning card, and a row of smalls — importance expressed by size, nothing a plain
grid would give.

The same grammar scales down the design into the other screens: the reciters are wide
rows with a monogram plate (no photos ship — the launcher stays offline-first, and the
plate is the honest way to keep every card equal); the reader's verses are calm full
rows; the adhkar are rows whose gold state ring marks today's completed zikr.

Everything draws from the launcher's own token system (`Tokens.java`): lapis-800 card
surfaces on the star-lattice ground, gold-400 accents and the chosen card's outline,
plaster-100 text, panel radius 24 for cards and row radius 16 for lists, the FocusTile
focus ring and its 1.03–1.06 scale — so the Library is visibly the same house the
arch and the courtyard are. RTL mirrors the whole grid exactly as Home mirrors
(`place()` measures from the start edge; the reader and the adhkar set their text
direction by language). Nothing new was invented in colour or type: Amiri carries the
sacred text, Alexandria the interface words, in the sizes the tokens name.

Focus: the bento tiles are `FocusTile`s, so focus moves with the remote over the grid
and every card carries the house focus ring; the header's gear opens the Library's own
settings; hints at the bottom follow the focused card, as they do on Home.
