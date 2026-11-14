package pro.datascoop.tv;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The recitation player: one chapter of the chosen reciter, streamed from mp3quran.net's servers or played
 * from the downloaded copy, kept alive across screens and pausing politely for the adhan — the same audio
 * focus rules the Quran radio follows, so the two never talk over each other or over the call to prayer.
 *
 * <p>While a chapter plays, its position is remembered every few seconds and on every pause or stop, so
 * Continue listening returns to the very spot. Repeat-one keeps the chapter going; auto-continue moves to the
 * next surah (1→114 then stops). Like the radio, it keeps the TV from switching itself off mid-recitation.
 */
final class Tilawa {
    static final int OFF = 0, CONNECTING = 1, PLAYING = 2, PAUSED = 3, RETRYING = 4, FAILED = 5;

    /** Repeat: follow on to the next chapter (default), or repeat the chapter. */
    static final int FOLLOW = 0, REPEAT_ONE = 1, STOP_AFTER = 2;

    interface Listener {
        void onTilawa(int state);
    }

    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final List<Listener> listeners = new ArrayList<>();
    private static Context app;
    private static MediaPlayer player;
    private static AudioFocusRequest focus;
    private static WifiManager.WifiLock wifiLock;
    private static int state = OFF, failures;
    private static boolean wanted, pausedForFocus, prepared;
    private static String reciterId;
    private static int surah;
    private static int repeat = FOLLOW;
    /** The last position handed to the position saver; the saver runs every few seconds while playing. */
    private static long lastSave;
    private static boolean saverRunning;

    static int state() {
        return state;
    }

    static boolean on() {
        return wanted;
    }

    static boolean playing() {
        return state == PLAYING;
    }

    static String reciter() {
        return reciterId;
    }

    static int surah() {
        return surah;
    }

    static int repeatMode() {
        return repeat;
    }

    static void setRepeatMode(Context c, int mode) {
        repeat = mode;
        for (Listener l : new ArrayList<>(listeners)) l.onTilawa(state);
    }

    static void listen(Listener l) {
        if (!listeners.contains(l)) listeners.add(l);
    }

    static void unlisten(Listener l) {
        listeners.remove(l);
    }

    // ---------------------------------------------------------------- start / stop

    /** Begins a chapter (stopping the radio and any earlier recitation); atMs resumes a remembered spot. */
    static void start(Context c, String reciter, int surahNumber, int atMs) {
        app = c.getApplicationContext();
        if (Radio.on()) Radio.stop();
        wanted = true;
        pausedForFocus = false;
        failures = 0;
        reciterId = reciter;
        surah = surahNumber;
        keepAwake(true);
        AudioManager am = app.getSystemService(AudioManager.class);
        focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes())
            .setOnAudioFocusChangeListener(Tilawa::onFocus, handler)
            .build();
        if (am != null) am.requestAudioFocus(focus);
        connect(atMs);
    }

    /** Switches chapter (or reciter) without losing the focus and the wake arrangement. */
    static void switchTo(Context c, String reciter, int surahNumber) {
        if (!wanted) {
            start(c, reciter, surahNumber, 0);
            return;
        }
        reciterId = reciter;
        surah = surahNumber;
        failures = 0;
        pausedForFocus = false;
        connect(0);
    }

    static void stop() {
        if (!wanted) return;
        wanted = false;
        savePosition();
        release();
        abandonFocus();
        keepAwake(false);
        set(OFF);
    }

    static void pause() {
        if (state != PLAYING && state != CONNECTING) return;
        savePosition();
        if (player != null) {
            try {
                player.pause();
            } catch (RuntimeException ignored) {
            }
        }
        set(PAUSED);
    }

    static void resume() {
        if (state != PAUSED) return;
        if (player != null) {
            try {
                player.start();
                set(PLAYING);
                return;
            } catch (RuntimeException ignored) {
            }
        }
        // The player is gone (an error while paused): reconnect where it stopped.
        int at = currentPos();
        connect(at);
    }

    // ---------------------------------------------------------------- position / seek

    static int position() {
        return currentPos();
    }

    static int duration() {
        try {
            return player == null ? 0 : player.getDuration();
        } catch (RuntimeException e) {
            return 0;
        }
    }

    static void seek(int toMs) {
        try {
            if (player != null && prepared) player.seekTo(Math.max(0, Math.min(duration(), toMs)));
        } catch (RuntimeException ignored) {
        }
        savePosition();
    }

    /** The remembered spot for (reciter, surah) — what Continue listening resumes. */
    static int savedPos(Context c, String reciter, int surahNumber) {
        return UserDb.of(c).audioPos(reciter, surahNumber);
    }

    // ---------------------------------------------------------------- connection

    private static void connect(int atMs) {
        handler.removeCallbacks(reconnect);
        if (!wanted || app == null) return;
        release();
        set(failures == 0 ? CONNECTING : RETRYING);
        MediaPlayer mp = new MediaPlayer();
        player = mp;
        prepared = false;
        mp.setAudioAttributes(attributes());
        mp.setWakeMode(app, android.os.PowerManager.PARTIAL_WAKE_LOCK);
        mp.setOnPreparedListener(p -> {
            if (p != player) return;
            failures = 0;
            prepared = true;
            if (atMs > 0 && atMs < p.getDuration() - 2000) {
                try {
                    p.seekTo(atMs);
                } catch (RuntimeException ignored) {
                }
            }
            p.start();
            set(PLAYING);
        });
        mp.setOnCompletionListener(p -> {
            if (p != player) return;
            savePosition();
            if (repeat == REPEAT_ONE) {
                connect(0);
            } else if (repeat == FOLLOW && surah < Quran.SURAHS) {
                switchTo(app, reciterId, surah + 1);
            } else {
                UserDb.of(app).saveAudioPos(reciterId, surah, 0);
                stop();
            }
        });
        mp.setOnErrorListener((p, what, extra) -> {
            if (p == player) retry();
            return true;
        });
        try {
            mp.setDataSource(source());
            mp.prepareAsync();
        } catch (Exception e) {
            retry();
        }
    }

    /** The local downloaded file when it is complete, else the reciter's server. */
    private static String source() {
        Reciters.Reciter r = Reciters.get(app).of(reciterId);
        if (r == null) throw new IllegalStateException("no reciter " + reciterId);
        File local = Downloads.file(app, reciterId, surah);
        if (local != null && local.length() > 0
                && UserDb.of(app).downloadStatus(reciterId, surah) == UserDb.DL_DONE) {
            return local.getPath();
        }
        return r.url(surah);
    }

    private static void retry() {
        release();
        if (!wanted) return;
        failures++;
        set(failures >= 3 ? FAILED : RETRYING);
        if (failures < 3) handler.postDelayed(reconnect, Math.min(30_000L, 3_000L * failures));
    }

    private static final Runnable reconnect = () -> {
        if (wanted && failures > 0) connect(0);
    };

    private static void release() {
        handler.removeCallbacks(reconnect);
        if (player != null) {
            MediaPlayer p = player;
            player = null;
            prepared = false;
            new Thread(() -> {
                try {
                    p.reset();
                    p.release();
                } catch (RuntimeException ignored) {
                }
            }, "tilawa-release").start();
        }
    }

    // ---------------------------------------------------------------- focus

    private static void onFocus(int change) {
        switch (change) {
            case AudioManager.AUDIOFOCUS_LOSS:
                // Another app has the sound now: step aside and remember where we were.
                stop();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // The adhan, or a navigation voice: pause and come back after it.
                if (wanted && state == PLAYING) {
                    pausedForFocus = true;
                    pause();
                }
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                if (wanted && pausedForFocus) {
                    pausedForFocus = false;
                    resume();
                }
                break;
            default:
                break;
        }
    }

    private static void abandonFocus() {
        if (app != null && focus != null) {
            AudioManager am = app.getSystemService(AudioManager.class);
            if (am != null) am.abandonAudioFocusRequest(focus);
        }
        focus = null;
    }

    private static AudioAttributes attributes() {
        return new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build();
    }

    // ---------------------------------------------------------------- position saving

    private static int currentPos() {
        try {
            return player != null && prepared ? player.getCurrentPosition() : 0;
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private static void savePosition() {
        if (app != null && reciterId != null && surah > 0 && wanted && prepared) {
            UserDb.of(app).saveAudioPos(reciterId, surah, currentPos());
        }
    }

    private static final Runnable saver = new Runnable() {
        @Override
        public void run() {
            if (state == PLAYING && System.currentTimeMillis() - lastSave > 5000) {
                lastSave = System.currentTimeMillis();
                savePosition();
            }
            if (wanted && saverRunning) handler.postDelayed(this, 1000);
        }
    };

    // ---------------------------------------------------------------- staying awake

    /**
     * While a recitation plays the TV must not switch itself off, exactly as the radio arranges it; the saved
     * "put to sleep" value is shared with the radio's, because only one of them can ever be playing.
     */
    private static void keepAwake(boolean on) {
        if (app == null) return;
        try {
            if (on) {
                int now = android.provider.Settings.Secure.getInt(app.getContentResolver(), "sleep_timeout", -1);
                if (now != -1) {
                    Prefs.of(app).edit().putInt("sleep_saved", now).apply();
                    android.provider.Settings.Secure.putInt(app.getContentResolver(), "sleep_timeout", -1);
                }
            } else if (!Radio.on()) {
                Radio.restoreSleep(app);
            }
        } catch (SecurityException ignored) {
            // No WRITE_SECURE_SETTINGS grant: the TV's own timer stands.
        }
        WifiManager wm = app.getSystemService(WifiManager.class);
        if (on && wm != null && wifiLock == null) {
            wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "afnon:tilawa");
            wifiLock.setReferenceCounted(false);
            wifiLock.acquire();
        } else if (!on && wifiLock != null) {
            wifiLock.release();
            wifiLock = null;
        }
    }

    private static void set(int s) {
        state = s;
        if (s == PLAYING && !saverRunning) {
            saverRunning = true;
            handler.post(saver);
        }
        if (s == OFF) {
            saverRunning = false;
            handler.removeCallbacks(saver);
        }
        for (Listener l : new ArrayList<>(listeners)) l.onTilawa(s);
    }
}
