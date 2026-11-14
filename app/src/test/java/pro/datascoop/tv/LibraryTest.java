package pro.datascoop.tv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The Islamic Library's pure layers: the search normalisation folds diacritics and letter forms exactly the
 * way the database's search column was built; the khatm day arithmetic crosses months and years correctly;
 * and every new interface string answers in all five languages.
 */
public class LibraryTest {

    private static Text lang(int lang) {
        return new Text(lang, lang == Text.AR || lang == Text.UR);
    }

    // ---------------------------------------------------------------- normalisation

    @Test
    public void diacritics_are_folded_away() {
        assertEquals("قل هو الله احد", Quran.normalize("قُلْ هُوَ ٱللَّهُ أَحَدٌ"));
        // the dagger alif drops out here; the database's second search column carries it as ا
        assertEquals("الحمد لله رب العلمين", Quran.normalize("ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ"));
        assertEquals("بسم الله الرحمن الرحيم", Quran.normalize("بِّسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"));
    }

    @Test
    public void quran_marks_and_ornaments_fold_away() {
        assertEquals("الله", Quran.normalize("اللّٰه"));
        assertEquals("", Quran.normalize("\u06D6\u06D7\u06D8\u06D9\u06DA"));
        // the dagger alif is the alef of رحمن: conventionally typed searches spell it with ا
        assertEquals("الرحمن", Quran.normalize("ٱلرَّحْمَٰنِ"));
    }

    @Test
    public void latin_passes_through_lower_cased_and_arabic_letters_stay() {
        assertEquals("alfatiha", Quran.normalize("AlFatiha"));
        assertEquals("الفاتحه", Quran.normalize("الفاتحة"));   // ة folds to its base ه
    }

    @Test
    public void a_query_matches_the_way_the_database_column_was_built() {
        // the pipeline's own rule: ح→ح, ٱ→ا, diacritics dropped — so a search finds the verse
        assertTrue(Quran.normalize("قل هو الله احد").equals(Quran.normalize("قُلْ هُوَ ٱللَّهُ أَحَدٌ")));
    }

    // ---------------------------------------------------------------- day arithmetic

    @Test
    public void days_cross_months_and_years() {
        assertEquals(0, UserDb.daysBetween(20261130, 20261130));
        assertEquals(1, UserDb.daysBetween(20261130, 20261201));
        assertEquals(31, UserDb.daysBetween(20261130, 20261231));
        assertEquals(1, UserDb.daysBetween(20261231, 20270101));
        assertEquals(30, UserDb.daysBetween(20260101, 20260131));
        assertEquals(-1, UserDb.daysBetween(20261201, 20261130));
    }

    @Test
    public void a_plan_day_key_lands_on_the_right_date() {
        assertEquals(20261201, UserDb.dayKeyAfter(20261130, 1));
        assertEquals(20270101, UserDb.dayKeyAfter(20261231, 1));
        assertEquals(20270104, UserDb.dayKeyAfter(20261228, 7));
    }

    @Test
    public void day_keys_are_whole_days() {
        assertEquals(20260228, UserDb.dayKeyAfter(20260228, 0));
        assertEquals(20260301, UserDb.dayKeyAfter(20260228, 1));   // 2026 is not a leap year
        assertEquals(20250301, UserDb.dayKeyAfter(20250228, 1));   // 2025 is not either
    }

    // ---------------------------------------------------------------- adhkar counts the book printed

    @Test
    public void times_read_as_the_book_printed_them() {
        assertEquals("مرة", lang(Text.AR).adhkarTimes(1));
        assertEquals("once", lang(Text.EN).adhkarTimes(1));
        assertEquals("×33", new Text(Text.AR, false).adhkarTimes(33));
        assertEquals("×100", new Text(Text.EN, false).adhkarTimes(100));
        assertEquals("Une fois", lang(Text.FR).adhkarTimes(1));
        assertEquals("bir kez", lang(Text.TR).adhkarTimes(1));
        assertEquals("ایک بار", lang(Text.UR).adhkarTimes(1));
    }

    @Test
    public void eastern_digits_show_where_the_setting_says() {
        Text eastern = new Text(Text.AR, true), western = new Text(Text.AR, false);
        assertEquals("×٣", eastern.adhkarTimes(3));
        assertEquals("×3", western.adhkarTimes(3));
        assertEquals("٢ / ٣", eastern.adhkarXOf(2, 3));
        assertEquals("2 / 3", western.adhkarXOf(2, 3));
    }

    // ---------------------------------------------------------------- the library strings, all five

    @Test
    public void every_language_has_its_own_library_words() {
        assertNotEquals(lang(Text.AR).libTitle(), lang(Text.EN).libTitle());
        assertNotEquals(lang(Text.EN).libTitle(), lang(Text.FR).libTitle());
        assertNotEquals(lang(Text.FR).libTitle(), lang(Text.TR).libTitle());
        assertNotEquals(lang(Text.TR).libTitle(), lang(Text.UR).libTitle());
        assertEquals("Islamic Library", lang(Text.EN).libTitle());
        assertEquals("المكتبة الإسلامية", lang(Text.AR).libTitle());
        assertEquals("Bibliothèque islamique", lang(Text.FR).libTitle());
        assertEquals("İslam Kütüphanesi", lang(Text.TR).libTitle());
        assertEquals("اسلامی لائبریری", lang(Text.UR).libTitle());
    }

    @Test
    public void the_sections_and_reminders_have_all_five_words() {
        String[][] keys = {
            {lang(Text.AR).libMorning(), lang(Text.EN).libMorning(), lang(Text.FR).libMorning(), lang(Text.TR).libMorning(), lang(Text.UR).libMorning()},
            {lang(Text.AR).libEvening(), lang(Text.EN).libEvening(), lang(Text.FR).libEvening(), lang(Text.TR).libEvening(), lang(Text.UR).libEvening()},
            {lang(Text.AR).libAfter(), lang(Text.EN).libAfter(), lang(Text.FR).libAfter(), lang(Text.TR).libAfter(), lang(Text.UR).libAfter()},
            {lang(Text.AR).lsRemMorning(), lang(Text.EN).lsRemMorning(), lang(Text.FR).lsRemMorning(), lang(Text.TR).lsRemMorning(), lang(Text.UR).lsRemMorning()},
            {lang(Text.AR).khMarkToday(), lang(Text.EN).khMarkToday(), lang(Text.FR).khMarkToday(), lang(Text.TR).khMarkToday(), lang(Text.UR).khMarkToday()},
            {lang(Text.AR).plDownloaded(), lang(Text.EN).plDownloaded(), lang(Text.FR).plDownloaded(), lang(Text.TR).plDownloaded(), lang(Text.UR).plDownloaded()},
            {lang(Text.AR).lbOpen(), lang(Text.EN).lbOpen(), lang(Text.FR).lbOpen(), lang(Text.TR).lbOpen(), lang(Text.UR).lbOpen()},
        };
        for (String[] five : keys) {
            for (int i = 1; i < five.length; i++) assertNotEquals(five[0], five[i]);
        }
    }

    @Test
    public void the_due_words_exist_per_kind() {
        assertEquals("Time for the morning adhkar", lang(Text.EN).lbDue(LibReminders.MORNING));
        assertEquals("Time for the evening adhkar", lang(Text.EN).lbDue(LibReminders.EVENING));
        assertEquals("The after-prayer adhkar are due", lang(Text.EN).lbDue(LibReminders.AFTER_PRAYER));
        assertEquals("حان وقت أذكار الصباح", lang(Text.AR).lbDue(LibReminders.MORNING));
    }

    @Test
    public void quran_place_lines_carry_the_juz_and_page() {
        assertEquals("Juz 3 · page 42", lang(Text.EN).qrAt(3, 42));
        assertEquals("الجزء 3 · الصفحة 42", new Text(Text.AR, false).qrAt(3, 42));
        assertEquals("Cüz 3 · sayfa 42", lang(Text.TR).qrAt(3, 42));
    }

    // ---------------------------------------------------------------- bytes for humans

    @Test
    public void sizes_read_as_b_kb_mb() {
        assertEquals("512 B", Lib.bytes(lang(Text.EN), 512));
        assertEquals("1.5 MB", Lib.bytes(lang(Text.EN), 1572864));
        assertEquals("819 kB", Lib.bytes(lang(Text.EN), 819 * 1024));
    }
}
