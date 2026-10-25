package pro.datascoop.tv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The interface strings: every language answers with its own words, RTL covers Arabic and Urdu, Eastern digits
 * convert, and the compass sectors keep their order.
 */
public class TextTest {

    private static Text lang(int lang) {
        return new Text(lang, lang == Text.AR || lang == Text.UR);
    }

    @Test
    public void every_language_has_its_own_settings_word() {
        assertNotEquals(lang(Text.AR).settings(), lang(Text.EN).settings());
        assertNotEquals(lang(Text.EN).settings(), lang(Text.FR).settings());
        assertNotEquals(lang(Text.FR).settings(), lang(Text.TR).settings());
        assertNotEquals(lang(Text.TR).settings(), lang(Text.UR).settings());
        assertEquals("Réglages", lang(Text.FR).settings());
        assertEquals("Ayarlar", lang(Text.TR).settings());
        assertEquals("ترتیبات", lang(Text.UR).settings());
    }

    @Test
    public void rtl_covers_arabic_and_urdu_only() {
        assertTrue(lang(Text.AR).ar);
        assertTrue(lang(Text.UR).ar);
        assertFalse(lang(Text.EN).ar);
        assertFalse(lang(Text.FR).ar);
        assertFalse(lang(Text.TR).ar);
    }

    @Test
    public void eastern_digits_convert_every_digit() {
        Text t = new Text(Text.AR, true);
        assertEquals("٠١٢٣٤٥٦٧٨٩", t.d("0123456789"));
        assertEquals("الرياح ١٢ كم/س", t.d("الرياح 12 كم/س"));
        // The separator beside Eastern digits reads as a zero, so it becomes the Arabic comma.
        assertEquals("صافٍ، ٢٤°", t.d("صافٍ · ٢٤°"));
        // Western digits pass through untouched.
        assertEquals("Wind 12", new Text(Text.EN, false).d("Wind 12"));
    }

    @Test
    public void prayer_names_are_six_per_language() {
        for (int l = 0; l <= 4; l++) assertEquals(6, lang(l).prayerNames().length);
        assertEquals("الفجر", lang(Text.AR).prayerNames()[0]);
        assertEquals("İmsak", lang(Text.TR).prayerNames()[0]);
        assertEquals("فجر", lang(Text.UR).prayerNames()[0]);
    }

    @Test
    public void compass_sectors_stay_in_order() {
        Text t = lang(Text.EN);
        assertEquals("North", t.octant(0));
        assertEquals("North-east", t.octant(45));
        assertEquals("East", t.octant(90));
        assertEquals("South", t.octant(180));
        assertEquals("West", t.octant(270));
        assertEquals("North-west", t.octant(315));
        assertEquals("North", t.octant(360));      // wraps
        assertEquals("North", t.octant(-1));        // negative wraps too
    }

    @Test
    public void hijri_months_are_twelve_per_language() {
        for (int l = 0; l <= 4; l++) assertEquals(12, lang(l).hijriMonths().length);
        assertEquals("رمضان", lang(Text.AR).hijriMonths()[8]);
        assertEquals("Ramazan", lang(Text.TR).hijriMonths()[8]);
    }

    @Test
    public void countdown_uses_the_arabic_counts() {
        assertEquals("باقي على رمضان يوم", lang(Text.AR).countdown("رمضان", 1));
        assertEquals("باقي على رمضان يومان", lang(Text.AR).countdown("رمضان", 2));
        assertTrue(lang(Text.AR).countdown("رمضان", 5).contains("٥ أيام"));
        assertTrue(lang(Text.AR).countdown("رمضان", 23).contains("٢٣ يوماً"));
        assertEquals("Ramadan in 23 days", lang(Text.EN).countdown("Ramadan", 23));
    }

    @Test
    public void weather_lines_carry_all_parts() {
        Text t = new Text(Text.EN, false);
        assertEquals("Clear · 24° · Humidity 55%", t.weatherLine("Clear", "24", "Humidity 55%"));
        assertEquals("Clear · 24°", t.weatherLine("Clear", "24", ""));
    }
}
