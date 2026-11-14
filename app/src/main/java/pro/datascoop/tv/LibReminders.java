package pro.datascoop.tv;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Date;

/**
 * The Library's daily reminders, off by default, each in its own switch: the morning and evening adhkar, the
 * adhkar after each prayer (ten minutes behind it), the daily reading and the khatm day. They ride the same
 * 15-second watch the prayer reminders use — so they survive restarts, need no alarm and never interfere
 * with the adhan or the call screen (nothing of this shows while a call is due).
 *
 * <p>A reminder is a quiet card: on Home the launcher's own elegant card with Open, Later (it comes back in
 * half an hour) and Close; over another app a non-touchable banner that goes away on its own. No sound, no
 * waking of the screen, nothing that interrupts what is being watched.
 */
final class LibReminders {
    private LibReminders() {}

    /** The five kinds, in the order Settings shows them. */
    static final int MORNING = 0, EVENING = 1, AFTER_PRAYER = 2, QURAN = 3, KHATM = 4;

    /** How long after its minute a reminder still counts as due (the check runs every 15 s). */
    private static final long WINDOW_MS = 30 * 60_000L;
    /** How long "Later" pushes it away for. */
    private static final long LATER_MS = 30 * 60_000L;

    /** Set by Home while it is in front: the reminder becomes Home's own card. */
    interface HomeCard {
        void showLibraryReminder(int kind, int prayer);
    }

    static HomeCard home;

    /** The reminder due right now, or -1. Returns (kind, prayerIndex) through the array. */
    static int due(Context c, PrayerDay day, Date now, int[] out) {
        if (!Prefs.anyLibReminder(c)) return -1;
        int minute = now.getHours() * 60 + now.getMinutes();
        if (Prefs.libRemMorning(c) && dueAt(minute, Prefs.libRemMorningAt(c), now, c, "lib_m")) {
            out[0] = MORNING;
            out[1] = -1;
            return MORNING;
        }
        if (Prefs.libRemEvening(c) && dueAt(minute, Prefs.libRemEveningAt(c), now, c, "lib_e")) {
            out[0] = EVENING;
            out[1] = -1;
            return EVENING;
        }
        if (Prefs.libRemQuran(c) && dueAt(minute, Prefs.libRemQuranAt(c), now, c, "lib_q")) {
            out[0] = QURAN;
            out[1] = -1;
            return QURAN;
        }
        if (Prefs.libRemKhatm(c) && dueAt(minute, Prefs.libRemKhatmAt(c), now, c, "lib_k")) {
            out[0] = KHATM;
            out[1] = -1;
            return KHATM;
        }
        if (Prefs.libRemAfter(c)) {
            // ten minutes behind every prayer, once each
            for (int i = 0; i < 6; i++) {
                if (i == PrayerDay.SUNRISE) continue;
                long after = now.getTime() - day.times[i].getTime();
                if (after >= 10 * 60_000L && after < 10 * 60_000L + WINDOW_MS) {
                    String key = "lib_p" + i + "|" + day.times[i].getTime();
                    if (!Prefs.of(c).getString("lib_shown", "").equals(key)) {
                        Prefs.of(c).edit().putString("lib_shown", key).apply();
                        out[0] = AFTER_PRAYER;
                        out[1] = i;
                        return AFTER_PRAYER;
                    }
                }
            }
        }
        return -1;
    }

    /** Whether the day's minute is inside the reminder's window and it has not shown already. */
    private static boolean dueAt(int minute, int at, Date now, Context c, String key) {
        long since = (minute - at + 24 * 60) % (24L * 60) * 60_000L;
        if (since > WINDOW_MS) return false;
        String stamp = key + "|" + UserDb.day(now.getTime());
        if (Prefs.of(c).getString("lib_stamp", "").equals(stamp)) return false;
        long later = Prefs.of(c).getLong("lib_later", 0);
        if (later > now.getTime()) return false;
        Prefs.of(c).edit().putString("lib_stamp", stamp).apply();
        return true;
    }

    /** "Later": away for half an hour, then it may show again. */
    static void later(Context c) {
        Prefs.of(c).edit().putLong("lib_later", System.currentTimeMillis() + LATER_MS).apply();
    }

    /** Called by the watch every 15 seconds with the day; shows the card or the banner. */
    static void tick(AccessibilityService service, PrayerDay day, Date now) {
        int[] out = new int[2];
        int kind = due(service, day, now, out);
        if (kind < 0) return;
        if (home != null) {
            home.showLibraryReminder(kind, out[1]);
        } else {
            showBanner(service, kind, out[1], day, now);
        }
    }

    /** The quiet banner over another app: nothing touches it, nothing sounds, and it leaves on its own. */
    static void showBanner(AccessibilityService service, int kind, int prayer, PrayerDay day, Date now) {
        Ui.init(service);
        Text t = Screen.loadText(service);
        Typeface ui = service.getResources().getFont(R.font.alexandria_regular);
        Typeface display = service.getResources().getFont(t.ar ? R.font.amiri_bold : R.font.marcellus_regular);

        LinearLayout card = new LinearLayout(service);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(t.ar ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        card.setPadding(Ui.ipx(28), Ui.ipx(22), Ui.ipx(28), 0);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Tokens.LAPIS_900);
        bg.setCornerRadius(Ui.px(24));
        bg.setStroke(Ui.ipx(2), Tokens.GOLD_400);
        card.setBackground(bg);

        LinearLayout row = new LinearLayout(service);
        row.setGravity(Gravity.CENTER_VERTICAL);
        Icon book = new Icon(service, Icon.BOOK, 1.4f);
        book.setColor(Tokens.GOLD_400);
        row.addView(book, new LinearLayout.LayoutParams(Ui.ipx(48), Ui.ipx(60)));
        LinearLayout words = new LinearLayout(service);
        words.setOrientation(LinearLayout.VERTICAL);
        TextView label = text(service, ui, 20, Tokens.MIST_400, t.lbLibLabel());
        if (!t.ar) label.setLetterSpacing(0.2f);
        TextView title = text(service, display, 40, Tokens.PLASTER_100, titleOf(t, kind, prayer, day));
        words.addView(label);
        words.addView(title);
        Screen.lineHeight(title, 1.1f);
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(0, -2, 1);
        wlp.setMarginStart(Ui.ipx(20));
        row.addView(words, wlp);
        card.addView(row);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
            Ui.ipx(640), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | (t.ar ? Gravity.LEFT : Gravity.RIGHT);
        lp.x = Ui.ipx(96);
        lp.y = Ui.ipx(56);
        WindowManager wm = service.getSystemService(WindowManager.class);
        try {
            wm.addView(card, lp);
        } catch (RuntimeException e) {
            return;
        }
        new android.os.Handler(service.getMainLooper()).postDelayed(() -> {
            try {
                wm.removeView(card);
            } catch (RuntimeException ignored) {
            }
        }, 12_000L);
    }

    static void hideBanner() {
        // the banner removes itself; nothing else keeps one
    }

    /** The reminder's own words. */
    static String titleOf(Text t, int kind, int prayer, PrayerDay day) {
        switch (kind) {
            case MORNING: return t.lbDue(0);
            case EVENING: return t.lbDue(1);
            case AFTER_PRAYER: {
                String[] names = t.prayerNames();
                return t.lbDue(2) + t.sep() + names[prayer < 0 ? 0 : prayer];
            }
            case QURAN: return t.lbQuranDue();
            default: return t.lbKhatmDue();
        }
    }

    /** Where Open goes. */
    static IntentFor open(int kind) {
        switch (kind) {
            case MORNING: return new IntentFor(AdhkarActivity.class, "section", Azkar.MORNING);
            case EVENING: return new IntentFor(AdhkarActivity.class, "section", Azkar.EVENING);
            case AFTER_PRAYER: return new IntentFor(AdhkarActivity.class, "section", Azkar.AFTER_PRAYER);
            case QURAN: return new IntentFor(QuranIndexActivity.class, null, 0);
            default: return new IntentFor(KhatmActivity.class, null, 0);
        }
    }

    /** A small carrier: the class to open and the one extra it wants. */
    static final class IntentFor {
        final Class<?> cls;
        final String extra;
        final int value;

        IntentFor(Class<?> cls, String extra, int value) {
            this.cls = cls;
            this.extra = extra;
            this.value = value;
        }
    }

    /** Nothing to schedule: the watch finds them on its own, whatever restarts. */
    static void reschedule(Context c) {
        Prefs.of(c).edit().remove("lib_stamp").remove("lib_shown").apply();
    }

    private static TextView text(android.content.Context c, Typeface tf, float size, int color, CharSequence s) {
        TextView v = new TextView(c);
        v.setTypeface(tf);
        v.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, Ui.px(size));
        v.setTextColor(color);
        v.setText(s);
        return v;
    }
}
