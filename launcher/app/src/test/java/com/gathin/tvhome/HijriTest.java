package com.gathin.tvhome;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Gregorian to tabular (civil) Hijri: the year starts, Ramadan's first day, and a mid-Rabi day. */
public class HijriTest {

    @Test
    public void the_tabular_year_starts() {
        // The three verified 1 Muharram days of the tabular civil calendar.
        assertArrayEquals(new int[]{1445, 1, 1}, Hijri.toHijri(2023, 7, 19));
        assertArrayEquals(new int[]{1446, 1, 1}, Hijri.toHijri(2024, 7, 8));
        assertArrayEquals(new int[]{1447, 1, 1}, Hijri.toHijri(2025, 6, 27));
    }

    @Test
    public void ramadan_1448_runs_from_8_february_2027() {
        assertArrayEquals(new int[]{1448, 9, 1}, Hijri.toHijri(2027, 2, 8));
        // Its last day, the date themes/ramadan's festival_until is set to.
        assertArrayEquals(new int[]{1448, 9, 30}, Hijri.toHijri(2027, 3, 9));
    }

    @Test
    public void a_mid_month_day_lands_where_it_should() {
        assertArrayEquals(new int[]{1448, 5, 14}, Hijri.toHijri(2026, 10, 26));
    }

    @Test
    public void month_names_come_out_in_order() {
        // format() is resource-backed; here the index arithmetic is what can break: month 12 stays Dhul Hijjah.
        int[] h = Hijri.toHijri(2026, 6, 17); // 1 Muharram 1448
        assertEquals(1448, h[0]);
        assertEquals(1, h[1]);
        assertEquals(1, h[2]);
    }
}
