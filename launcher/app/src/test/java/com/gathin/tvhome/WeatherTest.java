package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** The weather.json the tools push: what parses, what does not, and the WMO code buckets. */
public class WeatherTest {

    @Test
    public void the_tools_shape_parses() {
        Weather.Data d = Weather.parse("{\"temp_c\": 18.4, \"code\": 1}");
        assertEquals(18.4, d.tempC, 0.001);
        assertEquals(1, d.code);
    }

    @Test
    public void a_negative_temperature_parses() {
        Weather.Data d = Weather.parse("{\"temp_c\": -3.0, \"code\": 71}");
        assertEquals(-3.0, d.tempC, 0.001);
        assertEquals(71, d.code);
    }

    @Test
    public void junk_yields_nothing() {
        assertNull(Weather.parse(null));
        assertNull(Weather.parse(""));
        assertNull(Weather.parse("not json"));
        assertNull(Weather.parse("{\"code\": 1}"));
        assertNull(Weather.parse("{\"temp_c\": 18.4}"));
    }

    @Test
    public void wmo_codes_land_in_the_right_word() {
        assertEquals("weather_clear", Weather.bucket(0));
        assertEquals("weather_clear", Weather.bucket(1));
        assertEquals("weather_partly", Weather.bucket(2));
        assertEquals("weather_cloudy", Weather.bucket(3));
        assertEquals("weather_cloudy", Weather.bucket(45));
        assertEquals("weather_drizzle", Weather.bucket(53));
        assertEquals("weather_rain", Weather.bucket(63));
        assertEquals("weather_rain", Weather.bucket(81));
        assertEquals("weather_snow", Weather.bucket(71));
        assertEquals("weather_snow", Weather.bucket(85));
        assertEquals("weather_thunder", Weather.bucket(95));
        assertEquals("weather_cloudy", Weather.bucket(99));
    }
}
