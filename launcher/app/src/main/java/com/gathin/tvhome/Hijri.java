package com.gathin.tvhome;

import android.content.res.Resources;

import java.util.Calendar;

/**
 * The Islamic (Hijri) date, tabular civil arithmetic, without ICU or network: the arithmetic is small and
 * stable, like {@link Shamsi}. The pure arithmetic calendar can sit a day either side of local moon
 * sighting, so a theme's {@code hijri_adjust} shifts it a day or two. Months are localized through
 * resources: transliterations in values/, Arabic names in values-ar/.
 */
final class Hijri {

    private static final int[] MONTHS = {
            R.string.hijri_month_muharram, R.string.hijri_month_safar,
            R.string.hijri_month_rabi_al_awwal, R.string.hijri_month_rabi_al_akhir,
            R.string.hijri_month_jumada_al_ula, R.string.hijri_month_jumada_al_akhira,
            R.string.hijri_month_rajab, R.string.hijri_month_shaban,
            R.string.hijri_month_ramadan, R.string.hijri_month_shawwal,
            R.string.hijri_month_dhu_al_qidah, R.string.hijri_month_dhu_al_hijjah};

    private Hijri() {
    }

    /** Today's {year, month 1-12, day} in the tabular Hijri calendar, shifted by {@code adjust} days. */
    static int[] today(int adjust) {
        Calendar c = Calendar.getInstance();
        if (adjust != 0) c.add(Calendar.DAY_OF_MONTH, adjust);
        return toHijri(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    /** e.g. "14 Rabi al-Akhir 1448" ("14 ربيع الآخر 1448هـ" in Arabic). */
    static String format(Resources res, int[] h) {
        return res.getString(R.string.hijri_date_format, h[2], res.getString(MONTHS[h[1] - 1]), h[0]);
    }

    /** Gregorian to tabular (civil) Hijri, the Kuwaiti algorithm. Returns {year, month, day}. */
    static int[] toHijri(int gy, int gm, int gd) {
        int jd = julianDay(gy, gm, gd);
        long l = jd - 1948440L + 10632;
        long n = (l - 1) / 10631;
        l = l - 10631 * n + 354;
        long j = (10985 - l) / 5316 * (50 * l / 17719) + (l / 5670) * (43 * l / 15238);
        l = l - (30 - j) / 15 * (17719 * j / 50) - (j / 16) * (15238 * j / 43) + 29;
        int m = (int) (24 * l / 709);
        int d = (int) (l - 709 * m / 24);
        int y = (int) (30 * n + j - 30);
        return new int[]{y, m, d};
    }

    /** The Julian day number of a Gregorian date (Fliegel & Van Flandern, truncating int division). */
    private static int julianDay(int gy, int gm, int gd) {
        int a = (gm - 14) / 12;
        return (1461 * (gy + 4800 + a)) / 4
                + (367 * (gm - 2 - 12 * a)) / 12
                - (3 * (gy + 4900 + a) / 100) / 4
                + gd - 32075;
    }
}
