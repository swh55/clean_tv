package pro.datascoop.tv;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * The bundled Qur'an: the Uthmani text of Tanzil (assets/quran.db, built by tools/quran-data), opened read-only
 * after a one-time copy out of the APK. 114 surahs, 6236 ayahs, the 604-page and 30-juz boundaries, and a
 * letters-only search column, so verse search works without the diacritics.
 *
 * <p>Opening is off the main thread: screens call {@link #ready} in onCreate and build their content when the
 * database answers; a surah's verses are then loaded on a worker thread and handed back on the main one. The
 * surah table (114 rows) is cached in memory after the first read.
 */
final class Quran {
    static final int SURAHS = 114, AYAHS = 6236, PAGES = 604, JUZ = 30;

    static final class Surah {
        final int number;
        final String name, nameEn, plain;
        final boolean meccan;
        final int ayahs;

        Surah(Cursor c) {
            number = c.getInt(0);
            name = c.getString(1);
            plain = c.getString(2);
            nameEn = c.getString(3);
            meccan = c.getInt(4) == 1;
            ayahs = c.getInt(5);
        }
    }

    static final class Ayah {
        final int id, surah, ayah, juz, page;
        final String text;

        Ayah(Cursor c) {
            id = c.getInt(0);
            surah = c.getInt(1);
            ayah = c.getInt(2);
            text = c.getString(3);
            juz = c.getInt(4);
            page = c.getInt(5);
        }
    }

    private static final Object lock = new Object();
    private static volatile Quran instance;
    /** Bump when assets/quran.db changes, so installed TVs recopy it. */
    static final int DB_VERSION = 2;

    /** The main-thread hand-off; made lazily so the class loads away from Android too (unit tests). */
    private static Handler main() {
        return Holder.HANDLER;
    }

    private static final class Holder {
        static final Handler HANDLER = new Handler(Looper.getMainLooper());
    }

    private final SQLiteDatabase db;
    private Surah[] surahs;

    /** The opened database, or null before {@link #ready} has answered. */
    static Quran opened() {
        return instance;
    }

    private Quran(SQLiteDatabase db) {
        this.db = db;
    }

    /** The database is opened (and copied out of the APK on the very first run) on a worker thread. */
    static void ready(Context c, Runnable then) {
        if (instance != null) {
            then.run();
            return;
        }
        new Thread(() -> {
            synchronized (lock) {
                if (instance == null) instance = open(c.getApplicationContext());
                Quran q = instance;
                main().post(then);
            }
        }, "quran-open").start();
    }

    private static Quran open(Context c) {
        // The copied database is stamped with this build's version; a new APK with a new database recopies it.
        android.content.SharedPreferences p = c.getSharedPreferences("afnon", Context.MODE_PRIVATE);
        File out = c.getDatabasePath("quran.db");
        boolean copy = !out.exists() || p.getInt("quran_db_ver", 0) != DB_VERSION;
        if (copy) {
            out.getParentFile().mkdirs();
            File tmp = new File(out.getPath() + ".new");
            try (InputStream in = c.getAssets().open("quran.db");
                 OutputStream os = new FileOutputStream(tmp)) {
                byte[] buf = new byte[65536];
                for (int n; (n = in.read(buf)) > 0; ) os.write(buf, 0, n);
            } catch (IOException e) {
                tmp.delete();
                throw new IllegalStateException("quran.db asset unreadable", e);
            }
            if (out.exists()) out.delete();
            if (!tmp.renameTo(out)) throw new IllegalStateException("quran.db copy failed");
            p.edit().putInt("quran_db_ver", DB_VERSION).apply();
        }
        return new Quran(SQLiteDatabase.openDatabase(out.getPath(), null,
            SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS));
    }

    /** The whole surah table, cached. */
    Surah[] surahs() {
        synchronized (this) {
            if (surahs != null) return surahs;
        }
        List<Surah> list = new ArrayList<>(114);
        try (Cursor c = db.rawQuery("SELECT number, name, name_plain, name_en, meccan, ayahs FROM surahs ORDER BY number", null)) {
            while (c.moveToNext()) list.add(new Surah(c));
        }
        Surah[] all = list.toArray(new Surah[0]);
        synchronized (this) {
            if (surahs == null) surahs = all;
        }
        return surahs;
    }

    Surah surah(int number) {
        return surahs()[number - 1];
    }

    /** One surah's verses, loaded on the caller's thread. */
    Ayah[] surahAyahs(int number) {
        List<Ayah> list = new ArrayList<>(128);
        try (Cursor c = db.rawQuery("SELECT id, surah, ayah, text, juz, page FROM ayahs WHERE surah=? ORDER BY ayah",
                new String[] {String.valueOf(number)})) {
            while (c.moveToNext()) list.add(new Ayah(c));
        }
        return list.toArray(new Ayah[0]);
    }

    /** Where a surah's verses sit: its first and last page and juz. */
    int[] surahPlace(int number) {
        try (Cursor c = db.rawQuery("SELECT MIN(page), MAX(page), MIN(juz), MAX(juz) FROM ayahs WHERE surah=?",
                new String[] {String.valueOf(number)})) {
            c.moveToFirst();
            return new int[] {c.getInt(0), c.getInt(1), c.getInt(2), c.getInt(3)};
        }
    }

    /** The first verse of a page, as (surah, ayah); null when the page is out of the mushaf. */
    int[] pageStart(int page) {
        try (Cursor c = db.rawQuery(
                "SELECT surah, ayah FROM ayahs WHERE id=(SELECT first_ayah FROM pages WHERE page=?)",
                new String[] {String.valueOf(page)})) {
            return c.moveToFirst() ? new int[] {c.getInt(0), c.getInt(1)} : null;
        }
    }

    /** Page and juz of a verse. */
    int[] placeOf(int surah, int ayah) {
        try (Cursor c = db.rawQuery("SELECT page, juz FROM ayahs WHERE surah=? AND ayah=?",
                new String[] {String.valueOf(surah), String.valueOf(ayah)})) {
            return c.moveToFirst() ? new int[] {c.getInt(0), c.getInt(1)} : new int[] {1, 1};
        }
    }

    /** Verse search without diacritics: the query is normalised the way the first search column was. */
    void search(String query, int limit, java.util.function.Consumer<Ayah> each) {
        String q = normalize(query);
        if (q.isEmpty()) return;
        // Two columns, both letters-only: the mushaf omits the dagger alif's letter where today's spelling
        // writes ا (عَٰلَمِينَ vs عالمين), so the second column reads the dagger as ا and either matches.
        try (Cursor c = db.rawQuery(
                "SELECT id, surah, ayah, text, juz, page FROM ayahs WHERE search LIKE ? OR search2 LIKE ? ORDER BY id LIMIT ?",
                new String[] {"%" + q + "%", "%" + q + "%", String.valueOf(limit)})) {
            while (c.moveToNext()) each.accept(new Ayah(c));
        }
    }

    /** The same letters-only folding the database's search column was built with (tools/quran-data). */
    static String normalize(String s) {
        if (s.isEmpty()) return s;
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= 0x064B && ch <= 0x065B) continue;           // harakat and the like
            if (ch == 0x0670 || ch == 0x0640) continue;           // dagger alif (see search2), tatweel
            if (ch >= 0x06D6 && ch <= 0x06ED) continue;            // Quranic annotation marks
            switch (ch) {
                case 0x0671:                                       // ٱ alef wasla
                case 0x0623: case 0x0625: case 0x0622:             // أ إ آ
                    b.append('\u0627');
                    continue;
                case 0x0649: b.append('\u064A'); continue;          // ى
                case 0x0629: b.append('\u0647'); continue;          // ة
                case 0x0624: b.append('\u0648'); continue;          // ؤ
                case 0x0626: b.append('\u064A'); continue;          // ئ
                default:
            }
            if (ch < 0x0600) {                                   // Latin letters pass through lower-cased
                b.append(Character.toLowerCase(ch));
            } else {
                b.append(ch);
            }
        }
        return b.toString();
    }

    /** The attribution the database itself carries (Tanzil notice, checksums); shown in Settings → About-ish places. */
    String meta(String key) {
        try (Cursor c = db.rawQuery("SELECT value FROM meta WHERE key=?", new String[] {key})) {
            return c.moveToFirst() ? c.getString(0) : "";
        }
    }

    /** Splits an adhkar text into its Quran-braced segments: "…{كذا}…" → (كذا, true) beside plain hadith words. */
    static List<Object[]> segments(String text) {
        List<Object[]> out = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            int open = text.indexOf('{', i);
            if (open < 0) {
                if (i < text.length()) out.add(new Object[] {text.substring(i), false});
                break;
            }
            int close = text.indexOf('}', open);
            if (close < 0) close = text.length();
            if (open > i) out.add(new Object[] {text.substring(i, open), false});
            out.add(new Object[] {text.substring(open + 1, close), true});
            i = close + 1;
        }
        if (out.isEmpty()) out.add(new Object[] {text, false});
        return out;
    }
}
