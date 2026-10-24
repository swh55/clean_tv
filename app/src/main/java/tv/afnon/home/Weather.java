package tv.afnon.home;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * The weather on Home: temperature, humidity and the sky's description for the saved city, from
 * api.open-meteo.com (no key, no account). Off by default in Afnon's settings; while on, Home refreshes it
 * at most every half hour and keeps the last reading in its prefs, so a TV that lost its network still
 * shows the last weather it had. The only request carries the city's coordinates — no identifiers.
 *
 * <p>Runs where it is asked for (Home's once-a-second tick), never on its own timer: the fetch happens on
 * a throwaway thread and the answer is posted back to the main looper.
 */
final class Weather {
    private Weather() {}

    /** The reading Home shows; null until the first fetch or with weather never fetched. */
    static final class Now {
        final double temp;
        final int humidity;
        final int code;
        final Date time;

        Now(double temp, int humidity, int code, Date time) {
            this.temp = temp;
            this.humidity = humidity;
            this.code = code;
            this.time = time;
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
            p.getInt("weather_code", 0), new Date(at));
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
            Now got = null;
            try {
                String url = "https://api.open-meteo.com/v1/forecast?latitude=" + city.lat + "&longitude="
                    + city.lng + "&current=temperature_2m,relative_humidity_2m,weather_code&timezone=auto";
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(10_000);
                try {
                    if (conn.getResponseCode() == 200) {
                        String body = read(conn.getInputStream());
                        JSONObject cur = new JSONObject(body).getJSONObject("current");
                        got = new Now(cur.getDouble("temperature_2m"), cur.getInt("relative_humidity_2m"),
                            cur.getInt("weather_code"), new Date());
                    }
                } finally {
                    conn.disconnect();
                }
            } catch (Exception ignored) {
                // Offline, or the service is out: Home keeps showing the last reading it has.
            }
            Now fresh = got;
            main.post(() -> {
                fetching = false;
                if (fresh != null) {
                    Prefs.of(app).edit()
                        .putLong("weather_time", fresh.time.getTime())
                        .putFloat("weather_temp", (float) fresh.temp)
                        .putInt("weather_humidity", fresh.humidity)
                        .putInt("weather_code", fresh.code)
                        .putString("weather_city", cityId)
                        .apply();
                    // The city changed while the fetch was out: drop the reading, the next tick fetches anew.
                    if (cityId.equals(Prefs.city(app).id)) on.weather(fresh);
                }
            });
        }, "afnon-weather").start();
    }

    private static String read(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
        in.close();
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
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
