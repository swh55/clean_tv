package com.gathin.tvhome;

import java.time.LocalDate;

/**
 * Prayer times computed on the TV from the sun's position (PrayTimes.org's algorithm), no network, the
 * way {@link Sun} computes sunrise and sunset for the sky. Latitude and longitude come from config.xml's
 * {@code latitude}/{@code longitude} (docs/sky.md), the convention from a theme's {@code prayer_method}.
 * No high-latitude adjustment: the intended homes are the ones the calendar was made for.
 */
final class PrayerTimes {

    /** Method parameters: the Fajr angle, the Isha angle, and Isha as minutes after Maghrib (0: angle). */
    private static final float[][] METHODS = {
            {18.0f, 17.0f, 0},  // MWL, the Muslim World League
            {15.0f, 15.0f, 0},  // ISNA, North America
            {19.5f, 17.5f, 0},  // Egypt
            {18.5f, 0, 90},     // Makkah, Umm al-Qura: Isha 90 min after Maghrib
            {18.0f, 18.0f, 0},  // Karachi
            {18.0f, 17.0f, 0},  // Turkey
    };
    static final String[] METHOD_NAMES = {"MWL", "ISNA", "Egypt", "Makkah", "Karachi", "Turkey"};

    private PrayerTimes() {
    }

    /** The method index for a theme's {@code prayer_method} name; anything unknown is MWL (0). */
    static int methodIndex(String name) {
        if (name != null) {
            String n = name.trim();
            for (int i = 0; i < METHOD_NAMES.length; i++) {
                if (METHOD_NAMES[i].equalsIgnoreCase(n)) return i;
            }
        }
        return 0;
    }

    /**
     * The day's {fajr, sunrise, dhuhr, asr, maghrib, isha} in hours for the TV's own time zone, or null
     * when the place cannot carry the arithmetic (a pole or an unusable latitude).
     */
    static double[] compute(LocalDate date, double latitude, double longitude, int method) {
        float[] m = METHODS[clampMethod(method)];
        double fajrAngle = m[0], ishaAngle = m[1];
        double ishaMinutes = m[2];

        double jd = julian(date) - longitude / (15.0 * 24.0);
        double fajr = sunAngleTime(jd, latitude, fajrAngle, 5.0 / 24.0, true);
        double sunrise = sunAngleTime(jd, latitude, 0.833, 6.0 / 24.0, true);
        double dhuhr = fixHour(midDay(jd, 12.0 / 24.0));
        double asr = asrTime(jd, latitude, 13.0 / 24.0);
        double maghrib = sunAngleTime(jd, latitude, 0.833, 18.0 / 24.0, false);
        double isha = ishaMinutes > 0
                ? maghrib + ishaMinutes / 60.0
                : sunAngleTime(jd, latitude, ishaAngle, 18.0 / 24.0, false);
        if (Double.isNaN(fajr) || Double.isNaN(isha)) return null;
        double tz = timeZoneOffsetHours();
        return new double[]{fajr + tz, sunrise + tz, dhuhr + tz, asr + tz, maghrib + tz, isha + tz};
    }

    /** The TV's zone offset in hours, DST included, so the line follows what the clock shows. */
    static double timeZoneOffsetHours() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        return (c.get(java.util.Calendar.ZONE_OFFSET) + c.get(java.util.Calendar.DST_OFFSET)) / 3_600_000.0;
    }

    private static int clampMethod(int method) {
        return Math.max(0, Math.min(METHODS.length - 1, method));
    }

    /** Local mean solar noon, in hours, at the fraction {@code t} of the day. */
    private static double midDay(double jd, double t) {
        return 12.0 - sunPosition(jd + t)[1];
    }

    /**
     * The time the sun sits at {@code angle} degrees (depression below the horizon, positive), in hours,
     * before ({@code before}) or after noon.
     */
    private static double sunAngleTime(double jd, double lat, double angle, double t, boolean before) {
        double decl = sunPosition(jd + t)[0];
        double noon = midDay(jd, t);
        double inner = (-Math.sin(Math.toRadians(angle)) - Math.sin(Math.toRadians(decl)) * Math.sin(Math.toRadians(lat)))
                / (Math.cos(Math.toRadians(decl)) * Math.cos(Math.toRadians(lat)));
        if (inner < -1.0 || inner > 1.0) return Double.NaN;
        double hours = Math.toDegrees(Math.acos(inner)) / 15.0;
        return before ? noon - hours : noon + hours;
    }

    /** The Asr time, shadow factor 1 (the Shafi'i, standard convention). */
    private static double asrTime(double jd, double lat, double t) {
        double decl = sunPosition(jd + t)[0];
        double angle = -Math.toDegrees(Math.atan(1.0 / (1.0 + Math.tan(Math.toRadians(Math.abs(lat - decl))))));
        return sunAngleTime(jd, lat, angle, t, false);
    }

    /** The sun's {declination, equation of time in hours} at the Julian moment {@code jd}. */
    private static double[] sunPosition(double jd) {
        double d = jd - 2451545.0;
        double g = fixAngle(357.529 + 0.98560028 * d);
        double q = fixAngle(280.459 + 0.98564736 * d);
        double l = fixAngle(q + 1.915 * Math.sin(Math.toRadians(g)) + 0.020 * Math.sin(Math.toRadians(2 * g)));
        double e = 23.439 - 0.00000036 * d;
        double decl = Math.toDegrees(Math.asin(Math.sin(Math.toRadians(e)) * Math.sin(Math.toRadians(l))));
        double ra = fixHour(Math.toDegrees(Math.atan2(
                Math.cos(Math.toRadians(e)) * Math.sin(Math.toRadians(l)),
                Math.cos(Math.toRadians(l)))) / 15.0);
        double eqt = q / 15.0 - fixHour(ra);
        return new double[]{decl, eqt};
    }

    private static double fixAngle(double a) {
        return a - 360.0 * Math.floor(a / 360.0);
    }

    private static double fixHour(double a) {
        return a - 24.0 * Math.floor(a / 24.0);
    }

    /** The Julian day of a date at 0h UT. */
    private static double julian(LocalDate date) {
        int y = date.getYear(), m = date.getMonthValue();
        if (m <= 2) {
            y -= 1;
            m += 12;
        }
        double a = Math.floor(y / 100.0);
        double b = 2 - a + Math.floor(a / 4.0);
        return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + date.getDayOfMonth() + b - 1524.5;
    }
}
