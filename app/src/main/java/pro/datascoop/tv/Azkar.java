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
 * The adhkar of Hisn al-Muslim (assets/azkar.json, built by tools/quran-data/build_azkar.py from the book's
 * digital text): morning, evening and after-prayer sections. Each zikr carries its own repetition count — only
 * where the book itself prints one — and its source line, verbatim; Quranic verses inside a zikr are the
 * braced {…} parts, which {@link Quran#segments} pulls out so they can be shown in the Qur'an style.
 *
 * <p>Everything is loaded once, on whichever thread asks first; the file is ~30 KB.
 */
final class Azkar {

    static final class Zikr {
        final String text, source, note;
        final int count;

        Zikr(JSONObject o) {
            text = o.optString("text", "");
            source = o.optString("source", "");
            note = o.optString("note", "");
            count = Math.max(1, o.optInt("count", 1));
        }
    }

    static final int MORNING = 0, EVENING = 1, AFTER_PRAYER = 2;

    private static final Object lock = new Object();
    private static volatile Azkar instance;

    private final Zikr[] morning, evening, afterPrayer;
    private final String source, intro;

    private Azkar(JSONObject root) {
        source = root.optString("source", "");
        intro = root.optString("intro", "");
        morning = read(root.optJSONArray("morning"));
        evening = read(root.optJSONArray("evening"));
        afterPrayer = read(root.optJSONArray("afterPrayer"));
    }

    private Azkar() {
        morning = evening = afterPrayer = new Zikr[0];
        // an unreadable asset must not crash the launcher
        source = "";
        intro = "";
    }

    private static Zikr[] read(JSONArray a) {
        if (a == null) return new Zikr[0];
        List<Zikr> out = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) {
            try {
                out.add(new Zikr(a.getJSONObject(i)));
            } catch (org.json.JSONException ignored) {
                // a malformed entry is skipped, never fatal
            }
        }
        return out.toArray(new Zikr[0]);
    }

    static Azkar get(Context c) {
        if (instance != null) return instance;
        synchronized (lock) {
            if (instance == null) {
                try {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader r = new BufferedReader(
                            new InputStreamReader(c.getAssets().open("azkar.json"), StandardCharsets.UTF_8))) {
                        for (String line; (line = r.readLine()) != null; ) sb.append(line);
                    }
                    instance = new Azkar(new JSONObject(sb.toString()));
                } catch (Exception e) {
                    instance = new Azkar();
                }
            }
        }
        return instance;
    }

    Zikr[] section(int which) {
        return which == MORNING ? morning : which == EVENING ? evening : afterPrayer;
    }

    String source() {
        return source;
    }

    String intro() {
        return intro;
    }
}
