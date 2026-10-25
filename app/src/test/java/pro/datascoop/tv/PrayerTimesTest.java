package pro.datascoop.tv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.Coordinates;
import com.batoulapps.adhan.Madhab;
import com.batoulapps.adhan.Qibla;

import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/** The prayer times, on the pure side of {@link PrayerDay} (no Android framework in sight). */
public class PrayerTimesTest {

    /** Damascus, the app's own city. adhan-java answers in the JVM's zone: pin it to the city's own. */
    @Before
    public void damascusTime() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Damascus"));
    }

    /** Damascus, the app's own city. */
    private static final Coordinates DAMASCUS = new Coordinates(33.5131, 36.2919);

    private static Date at(int year, int month, int day, int hour, int minute) {
        Calendar c = Calendar.getInstance();
        c.set(year, month - 1, day, hour, minute, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private static int hour(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.HOUR_OF_DAY);
    }

    @Test
    public void damascus_autumn_times_land_in_sane_hours() {
        // A real day (2026-10-24); every prayer keeps to the window its mosques there keep to.
        Date[] t = PrayerDay.compute(DAMASCUS, CalculationMethod.MUSLIM_WORLD_LEAGUE, Madhab.SHAFI,
            at(2026, 10, 24, 12, 0));
        assertTrue("Fajr 4-6h, was " + hour(t[0]), hour(t[0]) >= 4 && hour(t[0]) <= 6);
        assertTrue("Sunrise after Fajr", t[1].after(t[0]));
        assertTrue("Dhuhr 11-13h, was " + hour(t[2]), hour(t[2]) >= 11 && hour(t[2]) <= 13);
        assertTrue("Asr after Dhuhr", t[3].after(t[2]));
        assertTrue("Maghrib 17-19h, was " + hour(t[4]), hour(t[4]) >= 17 && hour(t[4]) <= 19);
        assertTrue("Isha after Maghrib", t[5].after(t[4]));
    }

    @Test
    public void times_sit_on_the_whole_minute() {
        // The whole-minute rule is what keeps "already shown" matching across checks.
        Date[] t = PrayerDay.compute(DAMASCUS, CalculationMethod.MUSLIM_WORLD_LEAGUE, Madhab.SHAFI,
            at(2026, 10, 24, 12, 0));
        for (int i = 0; i < 6; i++) assertEquals(0, t[i].getTime() % 60_000);
    }

    @Test
    public void hanafi_asr_comes_later_than_the_standard() {
        Date[] shafi = PrayerDay.compute(DAMASCUS, CalculationMethod.MUSLIM_WORLD_LEAGUE, Madhab.SHAFI,
            at(2026, 10, 24, 12, 0));
        Date[] hanafi = PrayerDay.compute(DAMASCUS, CalculationMethod.MUSLIM_WORLD_LEAGUE, Madhab.HANAFI,
            at(2026, 10, 24, 12, 0));
        assertTrue("Hanafi Asr is the shadow school, 40+ minutes later",
            hanafi[3].getTime() - shafi[3].getTime() >= 40 * 60_000L);
        // Every other time is untouched by the school.
        for (int i : new int[] {0, 1, 2, 4, 5}) assertEquals(shafi[i].getTime(), hanafi[i].getTime());
    }

    @Test
    public void qibla_from_damascus_points_south_southeast() {
        double bearing = new Qibla(DAMASCUS).direction;
        assertTrue("Qibla from Damascus is ~162°, was " + bearing, bearing > 155 && bearing < 170);
    }

    @Test
    public void makkah_faces_the_kaaba_from_its_own_city() {
        // The anchor: from Makkah's city centre the great circle to the Kaaba itself reads north-west.
        double bearing = new Qibla(new Coordinates(21.3891, 39.8579)).direction;
        assertEquals(318.5, bearing, 3.0);
    }
}
