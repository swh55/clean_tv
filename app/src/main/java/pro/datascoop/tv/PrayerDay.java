package pro.datascoop.tv;

import android.content.Context;
import android.icu.util.IslamicCalendar;

import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.CalculationParameters;
import com.batoulapps.adhan.Coordinates;
import com.batoulapps.adhan.Madhab;
import com.batoulapps.adhan.PrayerTimes;
import com.batoulapps.adhan.Qibla;
import com.batoulapps.adhan.data.DateComponents;

import java.util.Calendar;
import java.util.Date;

/** One moment's view of the prayer day: the six times, which prayer is current and which is next. */
final class PrayerDay {
    static final int FAJR = 0, SUNRISE = 1, DHUHR = 2, ASR = 3, MAGHRIB = 4, ISHA = 5;

    /** Today's times, except that after Isha the Fajr slot holds tomorrow's Fajr (it is the next prayer). */
    final Date[] times = new Date[6];
    /** Index of the current prayer, or -1 before Fajr and between Sunrise and Dhuhr. */
    final int nowIndex;
    /** Index of the next prayer (never Sunrise, which is a marker, not a prayer). */
    final int nextIndex;
    final Date nextTime;
    /** The Fajr that ends tonight's Suhoor: today's before dawn, otherwise tomorrow's. */
    final Date suhoorEnd;
    final boolean friday;
    final boolean ramadan;
    /** 0 = none, 1 = Eid al-Fitr (1 Shawwal), 2 = Eid al-Adha (10 Dhu al-Hijjah), by the Umm al-Qura calendar. */
    final int eid;
    /** The day of Arafah (9 Dhu al-Hijjah). */
    final boolean arafah;
    /** Days until the next first of Ramadan and the next day of Arafah (0 = today); large when far away. */
    final int daysToRamadan, daysToArafah;
    final int hijriDay, hijriMonth, hijriYear;
    final double qibla;

    /** With the saved location, method, Asr school and Hijri adjustment (Scoop TV Settings). */
    PrayerDay(Context c, Date now) {
        this(now, Prefs.where(c), Prefs.method(c), Prefs.madhab(c), Prefs.hijriOffset(c));
    }

    PrayerDay(Date now, Coordinates where, CalculationMethod method, Madhab madhab, int hijriOffset) {
        Date[] today = compute(where, method, madhab, now);
        System.arraycopy(today, 0, times, 0, 6);

        int latest = -1;
        for (int i = 0; i < 6; i++) if (!times[i].after(now)) latest = i;
        nowIndex = latest == SUNRISE ? -1 : latest;

        int next = -1;
        for (int i = 0; i < 6; i++) {
            if (i != SUNRISE && times[i].after(now)) { next = i; break; }
        }
        Calendar c = Calendar.getInstance();
        c.setTime(now);
        c.add(Calendar.DAY_OF_MONTH, 1);
        Date tomorrowFajr = compute(where, method, madhab, c.getTime())[FAJR];
        if (next == -1) {
            next = FAJR;
            times[FAJR] = tomorrowFajr;
        }
        nextIndex = next;
        nextTime = times[next];
        suhoorEnd = times[FAJR].after(now) ? times[FAJR] : tomorrowFajr;

        c.setTime(now);
        friday = c.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY;

        IslamicCalendar h = new IslamicCalendar();
        h.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_UMALQURA);
        h.setTime(now);
        if (hijriOffset != 0) h.add(IslamicCalendar.DAY_OF_MONTH, hijriOffset);
        hijriDay = h.get(IslamicCalendar.DAY_OF_MONTH);
        hijriMonth = h.get(IslamicCalendar.MONTH);
        hijriYear = h.get(IslamicCalendar.YEAR);
        ramadan = hijriMonth == IslamicCalendar.RAMADAN;
        eid = hijriMonth == IslamicCalendar.SHAWWAL && hijriDay == 1 ? 1
            : hijriMonth == IslamicCalendar.DHU_AL_HIJJAH && hijriDay == 10 ? 2 : 0;
        arafah = hijriMonth == IslamicCalendar.DHU_AL_HIJJAH && hijriDay == 9;
        daysToRamadan = hijriDaysUntil(h, IslamicCalendar.RAMADAN, 1);
        daysToArafah = hijriDaysUntil(h, IslamicCalendar.DHU_AL_HIJJAH, 9);

        qibla = new Qibla(where).direction;
    }

    /** The six times of one date, on the whole minute; pure Java, so the unit tests can pin it. */
    static Date[] compute(Coordinates where, CalculationMethod method, Madhab madhab, Date date) {
        CalculationParameters params = method.getParameters();
        params.madhab = madhab;
        PrayerTimes pt = new PrayerTimes(where, DateComponents.from(date), params);
        return new Date[] {minute(pt.fajr), minute(pt.sunrise), minute(pt.dhuhr), minute(pt.asr),
            minute(pt.maghrib), minute(pt.isha)};
    }

    /** Days from today to the next (month, day) of the Hijri calendar, 0 when it is today. */
    private static int hijriDaysUntil(IslamicCalendar now, int month, int day) {
        if (now.get(IslamicCalendar.MONTH) == month && now.get(IslamicCalendar.DAY_OF_MONTH) == day) return 0;
        IslamicCalendar target = (IslamicCalendar) now.clone();
        target.set(IslamicCalendar.DAY_OF_MONTH, 1);
        target.set(IslamicCalendar.MONTH, month);
        target.set(IslamicCalendar.DAY_OF_MONTH, day);
        if (target.getTimeInMillis() <= now.getTimeInMillis()) {
            target.add(IslamicCalendar.YEAR, 1);
            target.set(IslamicCalendar.MONTH, month);
            target.set(IslamicCalendar.DAY_OF_MONTH, day);
        }
        return (int) Math.round((target.getTimeInMillis() - now.getTimeInMillis()) / 86_400_000.0);
    }

    /**
     * The time on its whole minute. adhan-java keeps the milliseconds of the moment it was asked, so the same
     * prayer came out a little different on every check, and "already shown" never matched: the reminder and the
     * call kept coming back after being closed.
     */
    private static Date minute(Date d) {
        return new Date(d.getTime() / 60_000 * 60_000);
    }
}
