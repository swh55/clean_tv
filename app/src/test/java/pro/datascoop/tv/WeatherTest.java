package pro.datascoop.tv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

/** The weather's pure side: the Open-Meteo response parser and the unit conversion. */
public class WeatherTest {

    /** The shape the service really sends (Damascus, trimmed to the fields the app reads). */
    private static final String BODY = "{"
        + "\"current\": {"
        + "  \"temperature_2m\": 18.4, \"relative_humidity_2m\": 61, \"weather_code\": 3,"
        + "  \"is_day\": 1, \"wind_speed_10m\": 12.6, \"wind_direction_10m\": 315},"
        + "\"daily\": {"
        + "  \"weather_code\": [3, 61, 0],"
        + "  \"temperature_2m_min\": [11.2, 9.8, 8.1],"
        + "  \"temperature_2m_max\": [23.7, 17.3, 21.9],"
        + "  \"precipitation_probability_max\": [10, 80, 0]}}";

    @Test
    public void parse_reads_the_current_reading() throws Exception {
        Weather.Parsed p = Weather.parse(BODY);
        assertEquals(18.4, p.now.temp, 0.0001);
        assertEquals(61, p.now.humidity);
        assertEquals(13, p.now.wind);           // rounded from 12.6
        assertEquals(315, p.now.windDir);
        assertTrue("is_day 1 reads as day", p.now.day);
    }

    @Test
    public void parse_reads_three_forecast_days() throws Exception {
        Weather.Parsed p = Weather.parse(BODY);
        assertEquals(3, p.days.length);
        assertEquals(3, p.days[0].code);
        assertEquals(11, p.days[0].min);
        assertEquals(24, p.days[0].max);
        assertEquals(10, p.days[0].rain);
        assertEquals(80, p.days[1].rain);       // tomorrow: the rain the alert line watches
        assertEquals(5, Weather.group(p.days[1].code));   // 61 is rain
    }

    @Test
    public void night_comes_through_as_night() throws Exception {
        JSONObject body = new JSONObject(BODY);
        body.getJSONObject("current").put("is_day", 0);
        assertFalse(Weather.parse(body.toString()).now.day);
    }

    @Test
    public void a_missing_is_day_still_parses() throws Exception {
        // Older cached responses and hand-made fixtures may not carry it; day is the safe default.
        JSONObject body = new JSONObject(BODY);
        body.getJSONObject("current").remove("is_day");
        assertTrue(Weather.parse(body.toString()).now.day);
    }

    @Test
    public void weather_groups_map_the_codes() {
        assertEquals(0, Weather.group(0));    // clear
        assertEquals(1, Weather.group(2));    // partly cloudy
        assertEquals(2, Weather.group(3));    // overcast
        assertEquals(3, Weather.group(48));   // fog
        assertEquals(4, Weather.group(53));   // drizzle
        assertEquals(5, Weather.group(63));   // rain
        assertEquals(5, Weather.group(81));   // showers
        assertEquals(6, Weather.group(73));   // snow
        assertEquals(7, Weather.group(95));   // thunderstorm
        assertEquals(1, Weather.group(44));   // an unknown code falls back to partly cloudy
    }

    @Test
    public void temperature_conversion() {
        assertEquals(0, Weather.convert(0, false), 0.0001);
        assertEquals(32, Weather.convert(0, true), 0.0001);
        assertEquals(100, Weather.convert(100, false), 0.0001);
        assertEquals(212, Weather.convert(100, true), 0.0001);
        assertEquals(-40, Weather.convert(-40, true), 0.0001);   // the two scales cross here
    }
}
