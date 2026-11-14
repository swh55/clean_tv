package pro.datascoop.tv;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import java.io.File;
import java.util.List;

/**
 * Chapter downloads: the reciter's own mp3quran.net file per surah, fetched by the system's DownloadManager
 * only when asked for — nothing is fetched on its own — into the app's own external files, where space is
 * cheap and uninstall clears it. The registry in UserDb is what the screens read; a finished download is
 * marked DONE only when the system says it succeeded, because the player refuses to play half a chapter.
 */
final class Downloads {
    private Downloads() {}

    /** Where a chapter lives: files/quran/<reciter>/<NNN>.mp3, or null if it is not there. */
    static File file(Context c, String reciter, int surah) {
        File dir = dir(c, reciter);
        if (dir == null) return null;
        File f = new File(dir, String.format(java.util.Locale.US, "%03d.mp3", surah));
        return f.exists() && f.length() > 0 ? f : null;
    }

    private static File dir(Context c, String reciter) {
        File base = c.getExternalFilesDir(null);
        if (base == null) base = new File(c.getFilesDir(), "external");
        File dir = new File(base, "quran" + File.separator + reciter);
        if (!dir.exists() && !dir.mkdirs() && !dir.exists()) return null;
        return dir;
    }

    /** Asks the system to fetch one chapter; the registry notes the queue id until it finishes. */
    static void start(Context c, String reciter, int surah) {
        Reciters.Reciter r = Reciters.get(c).of(reciter);
        if (r == null) return;
        File target = new File(dir(c, reciter), String.format(java.util.Locale.US, "%03d.mp3", surah));
        if (target.exists()) target.delete();
        DownloadManager dm = c.getSystemService(DownloadManager.class);
        if (dm == null) {
            UserDb.of(c).markDownload(reciter, surah, UserDb.DL_FAILED, 0);
            return;
        }
        DownloadManager.Request q = new DownloadManager.Request(Uri.parse(r.url(surah)))
            .setDestinationUri(Uri.fromFile(target))
            .setAllowedOverMetered(true)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
            .setTitle("quran/" + reciter + "/" + surah);
        try {
            long id = dm.enqueue(q);
            UserDb.of(c).markDownload(reciter, surah, UserDb.DL_RUNNING, id);
        } catch (RuntimeException e) {
            UserDb.of(c).markDownload(reciter, surah, UserDb.DL_FAILED, 0);
        }
    }

    /** True while the system still has the chapter in its queue. */
    private static long queueId(Context c, String reciter, int surah) {
        if (UserDb.of(c).downloadStatus(reciter, surah) != UserDb.DL_RUNNING) return -1;
        for (int[] row : UserDb.of(c).downloads(reciter)) if (row[0] == surah) return row[1];
        return -1;
    }

    /** True while the system still has the chapter in its queue. */
    static boolean running(Context c, String reciter, int surah) {
        long id = queueId(c, reciter, surah);
        if (id <= 0) return false;
        DownloadManager dm = c.getSystemService(DownloadManager.class);
        if (dm == null) return false;
        try (Cursor cur = dm.query(new DownloadManager.Query().setFilterById(id))) {
            if (cur.moveToFirst()) {
                int status = cur.getInt(cur.getColumnIndex(DownloadManager.COLUMN_STATUS));
                return status == DownloadManager.STATUS_RUNNING || status == DownloadManager.STATUS_PAUSED
                    || status == DownloadManager.STATUS_PENDING;
            }
        } catch (RuntimeException ignored) {
        }
        return false;
    }

    /** Drops a downloaded chapter (the file and the registry row). */
    static void delete(Context c, String reciter, int surah) {
        File dir = dir(c, reciter);
        if (dir != null) {
            File f = new File(dir, String.format(java.util.Locale.US, "%03d.mp3", surah));
            if (f.exists()) f.delete();
        }
        UserDb.of(c).forgetDownload(reciter, surah);
    }

    /** How much of the chapter has arrived, 0..1; 1 when the download is complete. */
    static float progress(Context c, String reciter, int surah) {
        UserDb db = UserDb.of(c);
        if (db.downloadStatus(reciter, surah) == UserDb.DL_DONE) return 1f;
        long id = queueId(c, reciter, surah);
        if (id <= 0) return 0f;
        DownloadManager dm = c.getSystemService(DownloadManager.class);
        if (dm == null) return 0f;
        try (Cursor cur = dm.query(new DownloadManager.Query().setFilterById(id))) {
            if (cur.moveToFirst()) {
                int status = cur.getInt(cur.getColumnIndex(DownloadManager.COLUMN_STATUS));
                long sofar = cur.getLong(cur.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                long total = cur.getLong(cur.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                if (status == DownloadManager.STATUS_SUCCESSFUL) return 1f;
                return total <= 0 ? 0f : Math.max(0f, Math.min(0.99f, sofar / (float) total));
            }
        } catch (RuntimeException ignored) {
        }
        return 0f;
    }

    /** Marks a finished download DONE and a failed one FAILED, from the system's own record. */
    static void settle(Context c) {
        UserDb db = UserDb.of(c);
        for (Reciters.Reciter r : Reciters.get(c).all()) {
            for (int[] row : db.downloads(r.id)) {
                if (row[2] != UserDb.DL_RUNNING) continue;
                long id = row[1];
                DownloadManager dm = c.getSystemService(DownloadManager.class);
                if (dm == null) continue;
                try (Cursor cur = dm.query(new DownloadManager.Query().setFilterById(id))) {
                    if (!cur.moveToFirst()) continue;
                    int status = cur.getInt(cur.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    long bytes = cur.getLong(cur.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    if (status == DownloadManager.STATUS_SUCCESSFUL && bytes > 0) {
                        db.markDownload(r.id, row[0], UserDb.DL_DONE, bytes);
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        db.markDownload(r.id, row[0], UserDb.DL_FAILED, 0);
                    }
                } catch (RuntimeException ignored) {
                }
            }
        }
    }

    /** Listens for the system's "download finished" and settles that one download at once. */
    static final class Finisher extends BroadcastReceiver {
        @Override
        public void onReceive(Context c, Intent i) {
            if (!DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(i.getAction())) return;
            long id = i.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (id <= 0) return;
            DownloadManager dm = c.getSystemService(DownloadManager.class);
            if (dm == null) return;
            try (Cursor cur = dm.query(new DownloadManager.Query().setFilterById(id))) {
                if (!cur.moveToFirst()) return;
                int stat = cur.getInt(cur.getColumnIndex(DownloadManager.COLUMN_STATUS));
                long bytes = cur.getLong(cur.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                String uri = cur.getString(cur.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                // Our own files: …/quran/<reciter>/<NNN>.mp3
                if (uri == null) return;
                List<String> segs = Uri.parse(uri).getPathSegments();
                if (segs.size() < 3 || !"quran".equals(segs.get(segs.size() - 3))) return;
                String reciter = segs.get(segs.size() - 2);
                int surah;
                try {
                    surah = Integer.parseInt(segs.get(segs.size() - 1).replace(".mp3", ""));
                } catch (NumberFormatException e) {
                    return;
                }
                if (stat == DownloadManager.STATUS_SUCCESSFUL && bytes > 0) {
                    UserDb.of(c).markDownload(reciter, surah, UserDb.DL_DONE, bytes);
                } else {
                    UserDb.of(c).markDownload(reciter, surah, UserDb.DL_FAILED, 0);
                    File f = file(c, reciter, surah);
                    if (f != null) f.delete();
                }
            } catch (RuntimeException ignored) {
            }
        }
    }
}
