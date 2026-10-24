package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.TimeZone;

/** Prayer times against known days for Damascus and Makkah, and the method table's behaviour. */
public class PrayerTimesTest {

    @Before
    public void damascusZone() {
        // The offsets the computation returns are the device's; pin the JVM to Damascus (UTC+3 all year
        // since October 2022) so the anchors below hold on any machine.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Damascus"));
    }

    private static double hours(int h, int m) {
        return h + m / 60.0;
    }

    private static void close(String what, double expected, double actual) {
        assertTrue(what + ": expected " + expected + " got " + actual,
                Math.abs(expected - actual) < 5.0 / 60.0);
    }

    @Test
    public void damascus_summer_solstice_mwl() {
        // 21 June 2025, Muslim World League: Fajr 03:42, Sunrise 05:25, Dhuhr 12:37, Asr 16:21,
        // Maghrib 19:48, Isha 21:25 local (UTC+3).
        double[] t = PrayerTimes.compute(LocalDate.of(2025, 6, 21), 33.5138, 36.2765,
                PrayerTimes.methodIndex("MWL"));
        assertNotNull(t);
        close("fajr", hours(3, 42), t[0]);
        close("sunrise", hours(5, 25), t[1]);
        close("dhuhr", hours(12, 37), t[2]);
        close("asr", hours(16, 21), t[3]);
        close("maghrib", hours(19, 48), t[4]);
        close("isha", hours(21, 25), t[5]);
    }

    @Test
    public void damascus_winter_solstice_mwl() {
        // 21 December 2025: Fajr 06:06, Sunrise 07:35, Dhuhr 12:33, Asr 15:13, Maghrib 17:31, Isha 18:55.
        double[] t = PrayerTimes.compute(LocalDate.of(2025, 12, 21), 33.5138, 36.2765, 0);
        close("fajr", hours(6, 6), t[0]);
        close("sunrise", hours(7, 35), t[1]);
        close("dhuhr", hours(12, 33), t[2]);
        close("maghrib", hours(17, 31), t[4]);
        close("isha", hours(18, 55), t[5]);
    }

    @Test
    public void makkah_isha_is_ninety_minutes_after_maghrib() {
        // Umm al-Qura: Isha is defined as a 90-minute interval, not an angle.
        double[] t = PrayerTimes.compute(LocalDate.of(2025, 6, 21), 21.4225, 39.8262,
                PrayerTimes.methodIndex("Makkah"));
        assertEquals(1.5, t[5] - t[4], 1.0 / 60.0);
    }

    @Test
    public void times_run_in_day_order() {
        double[] t = PrayerTimes.compute(LocalDate.of(2026, 10, 26), 33.5138, 36.2765, 0);
        for (int i = 1; i < t.length; i++) assertTrue(t[i - 1] < t[i]);
    }

    @Test
    public void an_unknown_method_is_mwl() {
        assertEquals(0, PrayerTimes.methodIndex(null));
        assertEquals(0, PrayerTimes.methodIndex(""));
        assertEquals(0, PrayerTimes.methodIndex("nope"));
        assertEquals(2, PrayerTimes.methodIndex("Egypt"));
        assertEquals(3, PrayerTimes.methodIndex("makkah"));
    }

    @Test
    public void an_unusable_place_yields_nothing() {
        assertNull(PrayerTimes.compute(LocalDate.of(2025, 6, 21), 89.0, 0.0, 0));
    }
}
