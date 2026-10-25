package pro.datascoop.tv;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * The weather on Home: temperature, humidity or wind, the sky's description and a three-day forecast for the
 * saved city, from api.open-meteo.com (no key, no account). Off in Settings; while on, Home refreshes it at most
 * every half hour and keeps the last reading in its prefs, so a TV that lost its network still shows the last
 * weather it had. The only request carries the city's coordinates — no identifiers.
 *
 * <p>Runs where it is asked for (Home's once-a-second tick), never on its own timer: the fetch happens on
 * a throwaway thread and the answer is posted back to the main looper. {@link #parse} is pure, so the unit tests
 * can pin it to a real response.
 */
final class Weather {
    private Weather() {}

    /** The reading Home shows; null until the first fetch or with weather never fetched. */
    static final class Now {
        final double temp;
        final int humidity;
        final int code;
        /** Wind at ten metres, km/h, and its direction in degrees from true north. */
        final int wind;
        final int windDir;
        /** 1 while the sun is up at the city, 0 at night (Open-Meteo is_day). */
        final boolean day;
        final Date time;

        Now(double temp, int humidity, int code, int wind, int windDir, boolean day, Date time) {
            this.temp = temp;
            this.humidity = humidity;
            this.code = code;
            this.wind = wind;
            this.windDir = windDir;
            this.day = day;
            this.time = time;
        }
    }

    /** One forecast day: today first, then the two that follow. */
    static final class Day {
        final int code;
        /** Rounded low and high, in Celsius — display converts. */
        final int min, max;
        /** The day's top chance of rain (or drizzle or a storm), in percent. */
        final int rain;

        Day(int code, int min, int max, int rain) {
            this.code = code;
            this.min = min;
            this.max = max;
            this.rain = rain;
        }
    }

    /** What one response holds: the current reading and the three forecast days. */
    static final class Parsed {
        final Now now;
        final Day[] days;

        Parsed(Now now, Day[] days) {
            this.now = now;
            this.days = days;
        }
    }

    interface OnWeather {
        void weather(Now now);
    }

    private static final long REFRESH_MS = 30 * 60_000L;
    private static boolean fetching = false;

    /** The saved reading, or null when the feature never fetched (or the city changed since it did). */
    static Now current(Context c) {
        android.content.SharedPreferences p = Prefs.of(c);
        long at = p.getLong("weather_time", 0);
        if (at == 0) return null;
        String city = p.getString("weather_city", "");
        if (!city.isEmpty() && !city.equals(Prefs.city(c).id)) return null;
        return new Now(p.getFloat("weather_temp", 0), p.getInt("weather_humidity", 0),
            p.getInt("weather_code", 0), p.getInt("weather_wind", 0), p.getInt("weather_winddir", 0),
            p.getBoolean("weather_day", true), new Date(at));
    }

    /** The saved three-day forecast, or null when the feature never fetched (or the city changed). */
    static Day[] forecast(Context c) {
        android.content.SharedPreferences p = Prefs.of(c);
        long at = p.getLong("weather_time", 0);
        if (at == 0) return null;
        String city = p.getString("weather_city", "");
        if (!city.isEmpty() && !city.equals(Prefs.city(c).id)) return null;
        try {
            JSONArray a = new JSONArray(p.getString("weather_daily", "[]"));
            Day[] out = new Day[a.length()];
            for (int i = 0; i < out.length; i++) {
                JSONObject d = a.getJSONObject(i);
                out[i] = new Day(d.getInt("code"), d.getInt("min"), d.getInt("max"), d.getInt("rain"));
            }
            return out.length == 0 ? null : out;
        } catch (Exception broken) {
            return null;   // an older or damaged record: the next fetch rewrites it
        }
    }

    /**
     * Home's tick: shows the saved reading at once and, when it is older than half an hour (or the city
     * changed), asks for a fresh one. The callback always runs on the main thread, possibly twice: once with
     * the cache and once when the fetch lands.
     */
    static void update(Context c, OnWeather on) {
        Now now = current(c);
        if (now != null) on.weather(now);
        long at = now == null ? 0 : now.time.getTime();
        boolean stale = System.currentTimeMillis() - at > REFRESH_MS;
        if (stale && !fetching) fetch(c.getApplicationContext(), on);
    }

    private static void fetch(Context app, OnWeather on) {
        fetching = true;
        final Prefs.City city = Prefs.city(app);
        final String cityId = city.id;   // stamped on the reading, so a city changed mid-fetch is not mismatched
        final Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            Parsed got = null;
            try {
                String url = "https://api.open-meteo.com/v1/forecast?latitude=" + city.lat + "&longitude="
                    + city.lng + "&current=temperature_2m,relative_humidity_2m,weather_code,is_day,"
                    + "wind_speed_10m,wind_direction_10m&daily=weather_code,temperature_2m_max,temperature_2m_min,"
                    + "precipitation_probability_max&forecast_days=3&timezone=auto";
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(10_000);
                try {
                    if (conn.getResponseCode() == 200) got = parse(read(conn.getInputStream()));
                } finally {
                    conn.disconnect();
                }
            } catch (Exception ignored) {
                // Offline, or the service is out: Home keeps showing the last reading it has.
            }
            Parsed fresh = got;
            main.post(() -> {
                fetching = false;
                if (fresh != null) {
                    Now n = fresh.now;
                    android.content.SharedPreferences.Editor e = Prefs.of(app).edit()
                        .putLong("weather_time", n.time.getTime())
                        .putFloat("weather_temp", (float) n.temp)
                        .putInt("weather_humidity", n.humidity)
                        .putInt("weather_code", n.code)
                        .putInt("weather_wind", n.wind)
                        .putInt("weather_winddir", n.windDir)
                        .putBoolean("weather_day", n.day)
                        .putString("weather_city", cityId);
                    JSONArray days = new JSONArray();
                    for (Day d : fresh.days) {
                        JSONObject o = new JSONObject();
                        try {
                            o.put("code", d.code).put("min", d.min).put("max", d.max).put("rain", d.rain);
                        } catch (Exception impossible) {
                            // An int never fails to go into a JSON object.
                        }
                        days.put(o);
                    }
                    e.putString("weather_daily", days.toString()).apply();
                    // The city changed while the fetch was out: drop the reading, the next tick fetches anew.
                    if (cityId.equals(Prefs.city(app).id)) on.weather(fresh.now);
                }
            });
        }, "afnon-weather").start();
    }

    /** The response body to the reading and the forecast; pure so the unit tests can pin it. */
    static Parsed parse(String body) throws Exception {
        JSONObject root = new JSONObject(body);
        JSONObject cur = root.getJSONObject("current");
        Now now = new Now(cur.getDouble("temperature_2m"), cur.getInt("relative_humidity_2m"),
            cur.getInt("weather_code"), (int) Math.round(cur.getDouble("wind_speed_10m")),
            cur.getInt("wind_direction_10m"), cur.optInt("is_day", 1) != 0, new Date());
        JSONObject daily = root.getJSONObject("daily");
        JSONArray codes = daily.getJSONArray("weather_code");
        JSONArray mins = daily.getJSONArray("temperature_2m_min");
        JSONArray maxs = daily.getJSONArray("temperature_2m_max");
        JSONArray rain = daily.getJSONArray("precipitation_probability_max");
        Day[] days = new Day[Math.min(3, codes.length())];
        for (int i = 0; i < days.length; i++) {
            days[i] = new Day(codes.getInt(i), (int) Math.round(mins.getDouble(i)),
                (int) Math.round(maxs.getDouble(i)), rain.isNull(i) ? 0 : rain.getInt(i));
        }
        return new Parsed(now, days);
    }

    private static String read(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
        in.close();
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    /** Celsius to the display unit: Fahrenheit when asked, otherwise the value as it came. */
    static double convert(double celsius, boolean fahrenheit) {
        return fahrenheit ? celsius * 9 / 5 + 32 : celsius;
    }

    /** WMO weather-code 0–99 groups to a short description, in both languages; Text does the digits. */
    static int group(int code) {
        if (code == 0) return 0;                    // clear
        if (code == 1 || code == 2) return 1;       // partly cloudy
        if (code == 3) return 2;                    // overcast
        if (code == 45 || code == 48) return 3;     // fog
        if (code >= 51 && code <= 57) return 4;     // drizzle
        if ((code >= 61 && code <= 67) || (code >= 80 && code <= 82)) return 5; // rain
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) return 6;   // snow
        if (code >= 95) return 7;                   // thunderstorm
        return 1;
    }
}
