package pro.datascoop.tv;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The reciters the Library plays chapter by chapter (assets/reciters.json, built from mp3quran.net's own
 * reciter list — the same servers its site and apps stream from, Hafs 'murattal'). One file per chapter:
 * {server}{NNN}.mp3 with NNN the surah number, so the player streams a chapter and the download manager
 * stores exactly that file for offline listening. No image is shipped with a reciter — the cards show a
 * drawn monogram plate, which keeps the launcher offline-first.
 */
final class Reciters {

    static final class Reciter {
        final String id, ar, en, server;

        Reciter(JSONObject o) {
            id = o.optString("id", "");
            ar = o.optString("ar", "");
            en = o.optString("en", "");
            server = o.optString("server", "");
        }

        String name(Text t) {
            return t.ar ? ar : en;
        }

        /** The chapter file: stream URL and download target are the same shape. */
        String url(int surah) {
            return server + String.format(java.util.Locale.US, "%03d", surah) + ".mp3";
        }
    }

    private static final Object lock = new Object();
    private static volatile Reciters instance;

    private final Reciter[] list;

    private Reciters(Reciter[] list) {
        this.list = list;
    }

    static Reciters get(Context c) {
        if (instance != null) return instance;
        synchronized (lock) {
            if (instance == null) {
                List<Reciter> out = new ArrayList<>();
                try {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader r = new BufferedReader(
                            new InputStreamReader(c.getAssets().open("reciters.json"), StandardCharsets.UTF_8))) {
                        for (String line; (line = r.readLine()) != null; ) sb.append(line);
                    }
                    JSONArray a = new JSONObject(sb.toString()).getJSONArray("reciters");
                    for (int i = 0; i < a.length(); i++) out.add(new Reciter(a.getJSONObject(i)));
                } catch (Exception e) {
                    // an unreadable asset must not crash the launcher
                }
                instance = new Reciters(out.toArray(new Reciter[0]));
            }
        }
        return instance;
    }

    Reciter[] all() {
        return list;
    }

    int index(String id) {
        for (int i = 0; i < list.length; i++) if (list[i].id.equals(id)) return i;
        return 0;
    }

    Reciter of(String id) {
        return list.length == 0 ? null : list[index(id)];
    }
}
