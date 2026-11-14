package pro.datascoop.tv;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * The Library's own settings: the five daily reminders (each off by default, each with its own switch and
 * its own time — the after-prayer one rides ten minutes behind every prayer, so it has no time of its own),
 * the reader's text size, the adhkar counters' reset, and the download manager. Rows move with the arrows,
 * OK flips a switch or opens, ◀▶ step a time while its row is focused — the shape Scoop TV Settings uses.
 */
public class LibrarySettingsActivity extends Screen {

    private static final float ROW_H = 84, ROW_GAP = 10, ROWS_TOP = 200;
    private static final float ROW_W = 1728;

    private final List<Row> rows = new ArrayList<>();
    private LinearLayout hints;
    private TextView saved;
    private final Runnable hideSaved = () -> saved.animate().alpha(0f).setDuration(300);

    // ---------------------------------------------------------------- rows

    private abstract class Row {
        FocusTile tile;
        TextView label, help, value;
        LinearLayout control;

        abstract String label();

        String help() { return ""; }

        /** The row's right-hand value: the switch state or the stepper's. */
        String value() { return ""; }

        /** OK. */
        void open() {}

        /** ◀▶ while the row is focused. */
        void step(boolean forward) {}
    }

    /** A switch row (a reminder on/off). */
    private abstract class ToggleRow extends Row {
        abstract boolean on();

        abstract void set(boolean on);

        String value() { return on() ? t.libOn() : t.libOff(); }

        void open() {
            set(!on());
            showSaved();
        }
    }

    /** A time row (minutes past midnight, stepped in half hours by ◀▶). */
    private abstract class TimeRow extends Row {
        abstract int minutes();

        abstract void setMinutes(int m);

        String value() { return t.lsTime(minutes()); }

        void step(boolean forward) {
            int m = minutes() + (forward ? 30 : -30);
            if (m < 0) m += 24 * 60;
            if (m >= 24 * 60) m -= 24 * 60;
            setMinutes(m);
            showSaved();
        }
    }

    // ---------------------------------------------------------------- screen

    @Override
    void build() {
        newRoot();

        FocusTile back = new FocusTile(this, 36);
        back.setRest(Tokens.LAPIS_800, Tokens.LINE_SOFT, 2);
        Icon backIcon = new Icon(this, Icon.BACK, 2f);
        back.addView(backIcon, new FrameLayout.LayoutParams(Ui.ipx(32), Ui.ipx(32), Gravity.CENTER));
        back.setLook(f -> backIcon.setColor(f ? Tokens.LAPIS_900 : Tokens.PLASTER_100));
        back.setContentDescription(t.back());
        back.setOnClickListener(v -> finish());
        placeTile(back, 96, 56, 72, 72);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(displayBold, 56, Tokens.PLASTER_100);
        title.setText(t.lsTitle());
        lineHeight(title, 1f);
        head.addView(title);
        TextView sub = text(ui400, 22, Tokens.MIST_400);
        sub.setText(t.lsRemindersHelp());
        head.addView(sub, lp(4, 0));
        head.setClipChildren(false);
        place(head, 96 + 72 + 28, 56, ViewGroup.LayoutParams.WRAP_CONTENT, 84);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(Ui.ipx(0), Ui.ipx(0), Ui.ipx(0), Ui.ipx(40));
        list.setClipChildren(false);
        scroll.addView(list, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        place(scroll, 96, ROWS_TOP, ROW_W, 940 - ROWS_TOP);

        // the reminder section
        list.addView(section(t.lsReminders()));
        rows.clear();
        add(list, new ToggleRow() {
            String label() { return t.lsRemMorning(); }
            String help() { return t.lsTime(Prefs.libRemMorningAt(c())) ; }
            boolean on() { return Prefs.libRemMorning(c()); }
            void set(boolean on) { Prefs.setLibRemMorning(c(), on); LibReminders.reschedule(c()); }
        });
        add(list, new TimeRow() {
            String label() { return t.lsRemMorning() + t.sep() + t.lsTimeRow(); }
            String help() { return t.lsRemindersHelp(); }
            int minutes() { return Prefs.libRemMorningAt(c()); }
            void setMinutes(int m) { Prefs.setLibRemMorningAt(c(), m); }
        });
        add(list, new ToggleRow() {
            String label() { return t.lsRemEvening(); }
            String help() { return t.lsTime(Prefs.libRemEveningAt(c())); }
            boolean on() { return Prefs.libRemEvening(c()); }
            void set(boolean on) { Prefs.setLibRemEvening(c(), on); LibReminders.reschedule(c()); }
        });
        add(list, new TimeRow() {
            String label() { return t.lsRemEvening() + t.sep() + t.lsTimeRow(); }
            String help() { return t.lsRemindersHelp(); }
            int minutes() { return Prefs.libRemEveningAt(c()); }
            void setMinutes(int m) { Prefs.setLibRemEveningAt(c(), m); }
        });
        add(list, new ToggleRow() {
            String label() { return t.lsRemAfter(); }
            String help() { return t.lsRemAfterHelp(); }
            boolean on() { return Prefs.libRemAfter(c()); }
            void set(boolean on) { Prefs.setLibRemAfter(c(), on); LibReminders.reschedule(c()); }
        });
        add(list, new ToggleRow() {
            String label() { return t.lsRemQuran(); }
            String help() { return t.lsTime(Prefs.libRemQuranAt(c())); }
            boolean on() { return Prefs.libRemQuran(c()); }
            void set(boolean on) { Prefs.setLibRemQuran(c(), on); LibReminders.reschedule(c()); }
        });
        add(list, new TimeRow() {
            String label() { return t.lsRemQuran() + t.sep() + t.lsTimeRow(); }
            String help() { return t.lsRemindersHelp(); }
            int minutes() { return Prefs.libRemQuranAt(c()); }
            void setMinutes(int m) { Prefs.setLibRemQuranAt(c(), m); }
        });
        add(list, new ToggleRow() {
            String label() { return t.lsRemKhatm(); }
            String help() { return t.lsTime(Prefs.libRemKhatmAt(c())); }
            boolean on() { return Prefs.libRemKhatm(c()); }
            void set(boolean on) { Prefs.setLibRemKhatm(c(), on); LibReminders.reschedule(c()); }
        });
        add(list, new TimeRow() {
            String label() { return t.lsRemKhatm() + t.sep() + t.lsTimeRow(); }
            String help() { return t.lsRemindersHelp(); }
            int minutes() { return Prefs.libRemKhatmAt(c()); }
            void setMinutes(int m) { Prefs.setLibRemKhatmAt(c(), m); }
        });

        // the reader & data section
        list.addView(section(t.libQuran()));
        add(list, new Row() {
            String label() { return t.lsFontSize(); }
            String value() { return t.d(String.valueOf(Prefs.quranFont(c()))); }
            void step(boolean forward) {
                Prefs.setQuranFont(c(), Math.max(1, Math.min(5, Prefs.quranFont(c()) + (forward ? 1 : -1))));
                showSaved();
            }
        });
        add(list, new Row() {
            String label() { return t.lsResetAzkar(); }
            String value() { return ""; }
            void open() {
                Lib.io(() -> {
                    UserDb.of(c()).azkarReset("m");
                    UserDb.of(c()).azkarReset("e");
                    UserDb.of(c()).azkarReset("p");
                }, null);
                showSaved();
            }
        });
        add(list, new Row() {
            String label() { return t.lsDownloads(); }
            String value() { return t.dlTitle(); }
            void open() { startSafely(new Intent(LibrarySettingsActivity.this, DownloadsActivity.class)); }
        });
        add(list, new Row() {
            String label() { return t.lsAzkarSource(); }
            String value() { return ""; }
        });

        saved = text(ui400, 22, Tokens.GOLD_400);
        saved.setAlpha(0f);
        place(saved, 96, 962, 400, 32);

        hints = new LinearLayout(this);
        hints.setOrientation(LinearLayout.HORIZONTAL);
        place(hints, 96, 1000, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        List<String> h = new ArrayList<>();
        h.add(t.hintSettingsLib());
        h.add(t.hintBackHome());
        setHints(hints, h);

        if (!rows.isEmpty()) rows.get(0).tile.requestFocus();
    }

    @Override
    void onTick() {
    }

    private android.content.Context c() {
        return this;
    }

    /** A section label above its rows. */
    private TextView section(String label) {
        TextView v = caps(text(ui400, 22, Tokens.MIST_400));
        v.setText(label);
        LinearLayout.LayoutParams slp = lp(28, 10);
        return v;
    }

    /** Builds one row's card and adds it: label and help at the start, the value at the end. */
    private void add(LinearLayout list, Row r) {
        rows.add(r);
        r.tile = new FocusTile(this, Tokens.RADIUS_ROW);
        r.tile.setRest(Tokens.LAPIS_800, Tokens.LINE_SOFT, 2);
        r.tile.setFocusScale(Tokens.FOCUS_SCALE_SETTINGS_ROW);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.ipx(30), Ui.ipx(16), Ui.ipx(30), Ui.ipx(14));
        r.label = text(ui500, 26, Tokens.PLASTER_100);
        r.label.setText(r.label());
        col.addView(r.label);
        r.help = text(ui400, 20, Tokens.MIST_400);
        String help = r.help();
        if (!help.isEmpty()) {
            r.help.setText(help);
            col.addView(r.help, lp(4, 0));
        }
        r.value = text(ui600, 26, Tokens.GOLD_400);
        r.value.setText(r.value());
        r.value.setPadding(Ui.ipx(8), 0, Ui.ipx(8), 0);
        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.HORIZONTAL);
        inner.setGravity(Gravity.CENTER_VERTICAL);
        inner.addView(col, new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(-2, -2);
        vlp.setMarginStart(Ui.ipx(16));
        inner.addView(r.value, vlp);
        r.tile.addView(inner, new FrameLayout.LayoutParams(-1, -2));

        r.tile.setLook(f -> {
            r.label.setTextColor(f ? Tokens.LAPIS_900 : Tokens.PLASTER_100);
            r.help.setTextColor(f ? Tokens.LAPIS_900 : Tokens.MIST_400);
            r.value.setTextColor(f ? Tokens.LAPIS_900 : Tokens.GOLD_400);
        });
        r.tile.setOnClickListener(v -> {
            r.open();
            refreshRow(r);
        });
        final Row row = r;
        r.tile.setOnFocusChangeListener((v, has) -> { if (has) refreshRow(row); });
        list.addView(r.tile, lp(0, ROW_GAP));
    }

    /** A row's value line, after a change. */
    private void refreshRow(Row r) {
        if (r.value == null) return;
        r.value.setText(r.value());
        String help = r.help();
        if (help.isEmpty()) r.help.setVisibility(View.GONE);
        else {
            r.help.setVisibility(View.VISIBLE);
            r.help.setText(help);
        }
    }

    private void showSaved() {
        for (Row r : rows) refreshRow(r);
        saved.setText(t.saved());
        saved.animate().alpha(1f).setDuration(200).start();
        saved.removeCallbacks(hideSaved);
        saved.postDelayed(hideSaved, 2000);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getAction() == KeyEvent.ACTION_DOWN) {
            View f = getCurrentFocus();
            Row row = null;
            for (Row r : rows) if (r.tile == f) row = r;
            if (row != null) {
                int k = e.getKeyCode();
                boolean left = k == KeyEvent.KEYCODE_DPAD_LEFT, right = k == KeyEvent.KEYCODE_DPAD_RIGHT;
                if (left || right) {
                    row.step((right != rtl));
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(e);
    }

    private LinearLayout.LayoutParams lp(float top, float bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = Ui.ipx(top);
        lp.bottomMargin = Ui.ipx(bottom);
        return lp;
    }
}
