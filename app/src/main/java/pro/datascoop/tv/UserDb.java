package pro.datascoop.tv;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * The Library's own data: where reading stopped, the audio position per reciter and chapter, bookmarks, the
 * khatm plans and their day-by-day log, the download registry and the adhkar counters. All of it is user data,
 * so it lives in a small SQLite file of its own — never in SharedPreferences, whose memory is no place for
 * logs and lists; and nothing heavy ever runs on the UI thread (callers use Lib.io).
 */
final class UserDb extends SQLiteOpenHelper {

    private static final String NAME = "library.db";
    private static final int VERSION = 1;

    private static volatile UserDb instance;

    static UserDb of(Context c) {
        if (instance == null) {
            synchronized (UserDb.class) {
                if (instance == null) instance = new UserDb(c.getApplicationContext());
            }
        }
        return instance;
    }

    private UserDb(Context c) {
        super(c, NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE read_pos(id INTEGER PRIMARY KEY CHECK(id=1), surah INT, ayah INT, page INT, juz INT, updated INT)");
        db.execSQL("CREATE TABLE audio_pos(reciter TEXT, surah INT, position INT, updated INT, PRIMARY KEY(reciter, surah))");
        db.execSQL("CREATE TABLE bookmarks(id INTEGER PRIMARY KEY AUTOINCREMENT, surah INT, ayah INT, created INT)");
        db.execSQL("CREATE TABLE khatm_plans(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, days INT, pages_per_day REAL, start_day INT, reminder INT, active INT, created INT)");
        db.execSQL("CREATE TABLE khatm_log(plan INT, day INT, pages INT, marked INT, PRIMARY KEY(plan, day))");
        db.execSQL("CREATE TABLE downloads(reciter TEXT, surah INT, bytes INT, status INT, PRIMARY KEY(reciter, surah))");
        db.execSQL("CREATE TABLE azkar_progress(section TEXT, day INT, idx INT, count INT, PRIMARY KEY(section, day, idx))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        // this is version 1
    }

    // ---------------------------------------------------------------- day key

    /** A day as the logs know it: YYYYMMDD of the given time. */
    static int day(long timeMs) {
        return Integer.parseInt(new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(new java.util.Date(timeMs)));
    }

    static int today() {
        return day(System.currentTimeMillis());
    }

    // ---------------------------------------------------------------- reading position

    /** Where the reader stopped: (surah, ayah, page, juz); NO_POS before the first read. */
    static final int[] NO_POS = new int[] {1, 1, 1, 1};

    int[] readPos() {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT surah, ayah, page, juz FROM read_pos WHERE id=1", null)) {
            return c.moveToFirst() ? new int[] {c.getInt(0), c.getInt(1), c.getInt(2), c.getInt(3)} : NO_POS;
        }
    }

    /** Whether any reading position has ever been saved. */
    boolean hasReadPos() {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT 1 FROM read_pos WHERE id=1", null)) {
            return c.moveToFirst();
        }
    }

    void saveReadPos(int surah, int ayah, int page, int juz) {
        ContentValues v = new ContentValues();
        v.put("id", 1);
        v.put("surah", surah);
        v.put("ayah", ayah);
        v.put("page", page);
        v.put("juz", juz);
        v.put("updated", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("read_pos", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    // ---------------------------------------------------------------- audio position

    /** Where listening stopped: the position in ms within (reciter, surah); 0 when never played. */
    int audioPos(String reciter, int surah) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT position FROM audio_pos WHERE reciter=? AND surah=?", new String[] {reciter, String.valueOf(surah)})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    void saveAudioPos(String reciter, int surah, int position) {
        ContentValues v = new ContentValues();
        v.put("reciter", reciter);
        v.put("surah", surah);
        v.put("position", position);
        v.put("updated", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("audio_pos", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /** The last listened chapter of a reciter: (surah, position), or null. */
    int[] lastListened(String reciter) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT surah, position FROM audio_pos WHERE reciter=? ORDER BY updated DESC LIMIT 1", new String[] {reciter})) {
            return c.moveToFirst() ? new int[] {c.getInt(0), c.getInt(1)} : null;
        }
    }

    // ---------------------------------------------------------------- bookmarks

    boolean bookmarked(int surah, int ayah) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT 1 FROM bookmarks WHERE surah=? AND ayah=?", new String[] {String.valueOf(surah), String.valueOf(ayah)})) {
            return c.moveToFirst();
        }
    }

    void toggleBookmark(int surah, int ayah) {
        if (bookmarked(surah, ayah)) {
            getWritableDatabase().delete("bookmarks", "surah=? AND ayah=?",
                    new String[] {String.valueOf(surah), String.valueOf(ayah)});
        } else {
            ContentValues v = new ContentValues();
            v.put("surah", surah);
            v.put("ayah", ayah);
            v.put("created", System.currentTimeMillis());
            getWritableDatabase().insert("bookmarks", null, v);
        }
    }

    /** All bookmarks, newest first: rows of (surah, ayah). */
    List<int[]> bookmarks() {
        List<int[]> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT surah, ayah FROM bookmarks ORDER BY created DESC", null)) {
            while (c.moveToNext()) out.add(new int[] {c.getInt(0), c.getInt(1)});
        }
        return out;
    }

    // ---------------------------------------------------------------- khatm

    static final class KhatmPlan {
        final int id, days, startDay;
        final float pagesPerDay;
        final boolean reminder, active;

        KhatmPlan(Cursor c) {
            id = c.getInt(0);
            days = c.getInt(1);
            pagesPerDay = c.getFloat(2);
            startDay = c.getInt(3);
            reminder = c.getInt(4) == 1;
            active = c.getInt(5) == 1;
        }

        /** The plan's day a given day-key falls on: 0 before the start, 1-based after it. */
        int dayNumber(int dayKey) {
            return (int) (daysBetween(startDay, dayKey) + 1);
        }

        /** The day-key the plan's nth day falls on. */
        int dayKey(int n) {
            return dayKeyAfter(startDay, n - 1);
        }
    }

    /** Whole days between two YYYYMMDD keys (negative when b is before a). */
    static int daysBetween(int a, int b) {
        try {
            java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US);
            java.util.Date da = f.parse(String.valueOf(a)), db = f.parse(String.valueOf(b));
            return (int) Math.round((db.getTime() - da.getTime()) / 86400000.0);
        } catch (java.text.ParseException e) {
            return 0;
        }
    }

    /** The day-key n days after a YYYYMMDD key. */
    static int dayKeyAfter(int a, int n) {
        try {
            java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US);
            java.util.Date d = f.parse(String.valueOf(a));
            d.setTime(d.getTime() + n * 86400000L);
            return Integer.parseInt(f.format(d));
        } catch (java.text.ParseException e) {
            return a;
        }
    }

    List<KhatmPlan> plans() {
        List<KhatmPlan> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, days, pages_per_day, start_day, reminder, active FROM khatm_plans ORDER BY created DESC", null)) {
            while (c.moveToNext()) out.add(new KhatmPlan(c));
        }
        return out;
    }

    long createPlan(int days, float pagesPerDay, int startDay, boolean reminder) {
        ContentValues v = new ContentValues();
        v.put("name", "");
        v.put("days", days);
        v.put("pages_per_day", pagesPerDay);
        v.put("start_day", startDay);
        v.put("reminder", reminder ? 1 : 0);
        v.put("active", 1);
        v.put("created", System.currentTimeMillis());
        return getWritableDatabase().insert("khatm_plans", null, v);
    }

    void deactivatePlan(int id) {
        getWritableDatabase().delete("khatm_plans", "id=?", new String[] {String.valueOf(id)});
        getWritableDatabase().delete("khatm_log", "plan=?", new String[] {String.valueOf(id)});
    }

    /** Pages read in a plan: the sum of its day entries (history kept, never rewritten). */
    int pagesRead(int plan) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT IFNULL(SUM(pages),0) FROM khatm_log WHERE plan=?", new String[] {String.valueOf(plan)})) {
            c.moveToFirst();
            return (int) Math.round(c.getDouble(0));
        }
    }

    /** Marks a day's reading done (the plan's quota), or adds extra pages to it. */
    void logPages(int plan, int dayKey, float pages) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT pages FROM khatm_log WHERE plan=? AND day=?", new String[] {String.valueOf(plan), String.valueOf(dayKey)})) {
            float now = c.moveToFirst() ? c.getFloat(0) : 0;
            ContentValues v = new ContentValues();
            v.put("plan", plan);
            v.put("day", dayKey);
            v.put("pages", now + pages);
            v.put("marked", System.currentTimeMillis());
            getWritableDatabase().insertWithOnConflict("khatm_log", null, v, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    /** The log rows of a plan: (dayKey, pages), for the progress detail. */
    List<long[]> log(int plan) {
        List<long[]> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT day, pages, marked FROM khatm_log WHERE plan=? ORDER BY day", new String[] {String.valueOf(plan)})) {
            while (c.moveToNext()) out.add(new long[] {c.getLong(0), (long) c.getFloat(1), c.getLong(2)});
        }
        return out;
    }

    /** Redistributes a plan's remaining pages over new days counting from today; the log stays as it is. */
    void redistribute(int plan, int days, float pagesPerDay, int startDay) {
        ContentValues v = new ContentValues();
        v.put("days", days);
        v.put("pages_per_day", pagesPerDay);
        v.put("start_day", startDay);
        getWritableDatabase().update("khatm_plans", v, "id=?", new String[] {String.valueOf(plan)});
    }

    void setPlanReminder(int plan, boolean on) {
        ContentValues v = new ContentValues();
        v.put("reminder", on ? 1 : 0);
        getWritableDatabase().update("khatm_plans", v, "id=?", new String[] {String.valueOf(plan)});
    }

    // ---------------------------------------------------------------- downloads

    static final int DL_RUNNING = 1, DL_DONE = 2, DL_FAILED = 3;

    void markDownload(String reciter, int surah, int status, long bytes) {
        ContentValues v = new ContentValues();
        v.put("reciter", reciter);
        v.put("surah", surah);
        v.put("bytes", bytes);
        v.put("status", status);
        getWritableDatabase().insertWithOnConflict("downloads", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    void forgetDownload(String reciter, int surah) {
        getWritableDatabase().delete("downloads", "reciter=? AND surah=?",
                new String[] {reciter, String.valueOf(surah)});
    }

    /** All downloads of a reciter: (surah, bytes, status). */
    List<int[]> downloads(String reciter) {
        List<int[]> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT surah, bytes, status FROM downloads WHERE reciter=? ORDER BY surah", new String[] {reciter})) {
            while (c.moveToNext()) out.add(new int[] {c.getInt(0), (int) c.getLong(1), c.getInt(2)});
        }
        return out;
    }

    int downloadStatus(String reciter, int surah) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT status FROM downloads WHERE reciter=? AND surah=?", new String[] {reciter, String.valueOf(surah)})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    // ---------------------------------------------------------------- adhkar counters

    /** Today's counter of one zikr in a section. */
    int azkarCount(String section, int dayKey, int idx) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT count FROM azkar_progress WHERE section=? AND day=? AND idx=?",
                new String[] {section, String.valueOf(dayKey), String.valueOf(idx)})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    /** Adds one to a zikr's counter (or sets it): the session progress of the day. */
    int azkarAdd(String section, int dayKey, int idx, int setTo) {
        int now = azkarCount(section, dayKey, idx);
        ContentValues v = new ContentValues();
        v.put("section", section);
        v.put("day", dayKey);
        v.put("idx", idx);
        v.put("count", setTo >= 0 ? setTo : now + 1);
        getWritableDatabase().insertWithOnConflict("azkar_progress", null, v, SQLiteDatabase.CONFLICT_REPLACE);
        return setTo >= 0 ? setTo : now + 1;
    }

    /** The whole section's progress for the day: counts by index. */
    int[] azkarProgress(String section, int dayKey, int size) {
        int[] out = new int[size];
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT idx, count FROM azkar_progress WHERE section=? AND day=?",
                new String[] {section, String.valueOf(dayKey)})) {
            while (c.moveToNext()) if (c.getInt(0) >= 0 && c.getInt(0) < size) out[c.getInt(0)] = c.getInt(1);
        }
        return out;
    }

    /** Resets a section's counters for the day (or the whole week if old rows are left behind). */
    void azkarReset(String section) {
        getWritableDatabase().delete("azkar_progress", "section=?", new String[] {section});
    }
}
