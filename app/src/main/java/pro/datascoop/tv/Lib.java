package pro.datascoop.tv;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** The Library's one background lane: every database and file touch runs here, never on the UI thread. */
final class Lib {
    private Lib() {}

    private static final ExecutorService lane = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "lib-io");
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });

    /** The main-thread hand-off; made lazily so the class loads away from Android too (unit tests). */
    private static Handler main() {
        return Holder.HANDLER;
    }

    private static final class Holder {
        static final Handler HANDLER = new Handler(Looper.getMainLooper());
    }

    /** Runs work off the main thread; done (if given) follows it on the main thread. */
    static void io(Runnable work, Runnable done) {
        lane.execute(() -> {
            work.run();
            if (done != null) main().post(done);
        });
    }

    static void io(Runnable work) {
        io(work, null);
    }

    static void main(Runnable r) {
        main().post(r);
    }

    /** A file size that reads in bytes, kB or MB with Western or Eastern digits. */
    static String bytes(Text t, long b) {
        if (b >= 1024 * 1024) return t.d(String.format(java.util.Locale.US, "%.1f MB", b / 1048576.0));
        if (b >= 1024) return t.d(String.format(java.util.Locale.US, "%d kB", b / 1024));
        return t.d(b + " B");
    }
}
