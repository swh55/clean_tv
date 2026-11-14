package pro.datascoop.tv;

import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * The Qur'an khatm plans: the ready plans (30, 60, 90 days) and a custom one (by pages or by juz a day, and
 * when it starts), the day's reading with its pages and where they fall in the mushaf, the plan's progress —
 * percentage, pages and juz done, days left, the expected finish — and the honest machinery: marking the day
 * is a button pressed by the reader, never anything automatic; falling behind shows the days behind and
 * offers to redistribute the remaining pages over a fresh count of days without touching the history.
 */
public class KhatmActivity extends Screen {

    private LinearLayout list;
    private LinearLayout hints;

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
        title.setText(t.libKhatm());
        lineHeight(title, 1f);
        head.addView(title);
        head.setClipChildren(false);
        place(head, 96 + 72 + 28, 56, ViewGroup.LayoutParams.WRAP_CONTENT, 84);

        FocusTile create = chip(Icon.STAR, t.libNew());
        create.setOnClickListener(v -> openCreate());
        placeEnd(create, 96, 56, ViewGroup.LayoutParams.WRAP_CONTENT, 72);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(Ui.ipx(8), Ui.ipx(4), Ui.ipx(8), Ui.ipx(48));
        list.setClipChildren(false);
        scroll.addView(list, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        place(scroll, 96, 200, 1728, 740);

        hints = new LinearLayout(this);
        hints.setOrientation(LinearLayout.HORIZONTAL);
        place(hints, 96, 962, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setHints(hints, hints2());

        refresh();
    }

    private List<String> hints2() {
        List<String> h = new ArrayList<>();
        h.add(t.hintKhatm());
        h.add(t.hintBackHome());
        return h;
    }

    // ---------------------------------------------------------------- rendering

    private void refresh() {
        final KhatmActivity self = this;
        Lib.io(() -> {
            final List<UserDb.KhatmPlan> plans = UserDb.of(this).plans();
            final List<Integer> read = new ArrayList<>();
            for (UserDb.KhatmPlan p : plans) read.add(UserDb.of(this).pagesRead(p.id));
            Lib.main(() -> {
                if (self.isFinishing() || list == null) return;
                list.removeAllViews();
                if (plans.isEmpty()) {
                    TextView none = text(ui400, 26, Tokens.MIST_400);
                    none.setText(t.khNone());
                    none.setPadding(Ui.ipx(16), Ui.ipx(16), Ui.ipx(16), Ui.ipx(16));
                    list.addView(none, lp(24, 24));
                }
                for (int i = 0; i < plans.size(); i++) list.addView(planCard(plans.get(i), read.get(i)), lp(0, 16));
                if (list.getChildCount() > 0 && list.getChildAt(0) instanceof FocusTile) list.getChildAt(0).requestFocus();
            });
        }, null);
    }

    /** One plan's card: its shape, its progress bar, its day, its buttons. */
    private View planCard(UserDb.KhatmPlan p, int pages) {
        FocusTile tile = new FocusTile(this, Tokens.RADIUS_ROW);
        tile.setRest(Tokens.LAPIS_800, Tokens.LINE_SOFT, 2);
        tile.setFocusScale(Tokens.FOCUS_SCALE_ROW);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.ipx(32), Ui.ipx(26), Ui.ipx(32), Ui.ipx(22));
        col.setClipChildren(false);

        int today = UserDb.today();
        int pct = (int) Math.round(pages * 100.0 / Quran.PAGES);
        int day = p.dayNumber(today);
        int daysLeft = Math.max(0, p.days - Math.max(1, day) + 1);
        int expectedEnd = UserDb.dayKeyAfter(p.startDay, Math.max(0, p.days - 1));
        int doneDays = Math.max(0, p.dayNumber(today) - 1);
        int behind = pages < Math.round(p.pagesPerDay * doneDays)
            ? Math.max(1, (int) Math.ceil((Math.round(p.pagesPerDay * doneDays) - pages) / Math.max(1, p.pagesPerDay))) : 0;

        // the title row: the plan's shape and its progress
        TextView title = text(ui500, 26, Tokens.GOLD_400);
        title.setText(t.khDays(p.days) + t.sep() + t.khPagesDaily((int) Math.round(p.pagesPerDay)));
        col.addView(title);
        TextView line = text(display, 64, Tokens.PLASTER_100);
        line.setText(t.khProgress(pct));
        line.setTextDirection(View.TEXT_DIRECTION_LTR);
        lineHeight(line, 1f);
        col.addView(line, lp(6, 0));
        TextView pagesLine = text(ui400, 22, Tokens.MIST_400);
        pagesLine.setText(t.khOfPages(pages, Quran.PAGES) + t.sep() + t.khJuzLeft((int) Math.ceil((Quran.PAGES - pages) / 20.13)));
        col.addView(pagesLine, lp(6, 0));

        // the progress bar
        LinearLayout track = new LinearLayout(this);
        track.setBackgroundColor(Tokens.BANNER_TRACK);
        View fill = new View(this);
        fill.setBackgroundColor(Tokens.GOLD_400);
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(Math.round(Ui.ipx(1664 - 64) * Math.max(0, Math.min(1, pages / (float) Quran.PAGES))), -1);
        track.addView(fill, flp);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-1, Ui.ipx(8));
        tlp.topMargin = Ui.ipx(14);
        col.addView(track, tlp);

        // the day's own reading
        TextView dayLine = text(ui500, 24, Tokens.PLASTER_100);
        int from = pages + 1, to = Math.min(Quran.PAGES, pages + Math.round(p.pagesPerDay));
        dayLine.setText(t.khTodayRange(from, to) + t.sep() + t.d("…"));
        whereToday(pages, dayLine);
        col.addView(dayLine, lp(16, 0));

        // days left, expected finish, behind?
        TextView status = text(ui400, 22, Tokens.MIST_400);
        String end = new java.text.SimpleDateFormat("d MMMM yyyy", Screen.dateLocale(t)).format(new java.util.Date());
        try {
            java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US);
            end = new java.text.SimpleDateFormat("d MMMM yyyy", Screen.dateLocale(t)).format(f.parse(String.valueOf(expectedEnd)));
        } catch (java.text.ParseException ignored) {
        }
        status.setText(t.khDaysLeft(daysLeft) + t.sep() + t.khExpected(end)
            + (behind > 0 ? t.sep() + t.khBehind(behind) : ""));
        col.addView(status, lp(8, 0));

        // the buttons: mark today done, add pages, redistribute, delete
        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setClipChildren(false);
        float r = FocusTile.REACH;
        FocusTile mark = pill(Icon.CHECK, t.khMarkToday());
        mark.setOnClickListener(v -> {
            Lib.io(() -> UserDb.of(this).logPages(p.id, today, Math.round(p.pagesPerDay)), null);
            refresh();
        });
        FocusTile add = pill(Icon.PLAY, t.khAddPages(5));
        add.setOnClickListener(v -> {
            Lib.io(() -> UserDb.of(this).logPages(p.id, today, 5), null);
            refresh();
        });
        FocusTile redis = pill(Icon.BOOK, t.khRedistribute());
        redis.setOnClickListener(v -> redistribute(p, pages));
        FocusTile del = pill(Icon.STOP, t.khDelete());
        del.setOnClickListener(v -> {
            Lib.io(() -> UserDb.of(this).deactivatePlan(p.id), null);
            refresh();
        });
        buttons.addView(mark, pillLp());
        buttons.addView(add, pillLp());
        buttons.addView(redis, pillLp());
        buttons.addView(del, pillLp());
        col.addView(buttons, lp(20, 0));

        tile.addView(col, new FrameLayout.LayoutParams(-1, -1));
        tile.setLook(f -> {
            title.setTextColor(f ? Tokens.LAPIS_900 : Tokens.GOLD_400);
            dayLine.setTextColor(f ? Tokens.LAPIS_900 : Tokens.PLASTER_100);
        });
        return tile;
    }

    /** Fills in today's pages in the mushaf: the surah and verse the day's first page starts at. */
    private void whereToday(int pagesRead, final TextView into) {
        int page = Math.min(Quran.PAGES, pagesRead + 1);
        Quran.ready(this, () -> {
            Quran q = Quran.opened();
            if (q == null || isFinishing() || into == null) return;
            int[] start = q.pageStart(page);
            if (start == null) return;
            Quran.Surah s = q.surah(start[0]);
            into.setText(into.getText() + t.sep() + (t.ar ? s.name : s.nameEn) + " " + t.qrAyahMark(start[1]));
        });
    }

    /** Redistributes the remaining pages over a fresh count of days from today; history stays. */
    private void redistribute(UserDb.KhatmPlan p, int pages) {
        int remaining = Math.max(0, Quran.PAGES - pages);
        if (remaining == 0) return;
        // the days already on the plan stay as they are; the rest are spread over a fresh count of
        // days at the same pace, so the expected finish moves out and the history is untouched
        int elapsed = Math.max(0, p.dayNumber(UserDb.today()) - 1);
        int newRemaining = Math.max(1, (int) Math.ceil(remaining / p.pagesPerDay));
        final int days = elapsed + newRemaining;
        final float perDay = (float) remaining / newRemaining;
        Lib.io(() -> UserDb.of(this).redistribute(p.id, days, perDay, p.startDay), this::refresh);
    }

    // ---------------------------------------------------------------- new plan

    /** The create panel: the three ready plans, or the custom one (pages or juz a day, start delay). */
    private void openCreate() {
        closeCreate();
        FrameLayout panel = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Tokens.LAPIS_700);
        bg.setCornerRadius(Ui.px(Tokens.RADIUS_PANEL));
        bg.setStroke(Ui.ipx(2), Tokens.GOLD_400_60);
        panel.setBackground(bg);
        panel.setPadding(Ui.ipx(40), Ui.ipx(34), Ui.ipx(40), Ui.ipx(30));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setClipChildren(false);

        TextView title = text(displayBold, 44, Tokens.PLASTER_100);
        title.setText(t.libNew());
        col.addView(title);

        LinearLayout presets = new LinearLayout(this);
        presets.setOrientation(LinearLayout.HORIZONTAL);
        presets.setClipChildren(false);
        int[] days = {30, 60, 90};
        for (int d : days) {
            FocusTile p = pill(Icon.BOOK, t.khDays(d));
            p.setOnClickListener(v -> {
                Lib.io(() -> UserDb.of(this).createPlan(d, (float) Quran.PAGES / d, UserDb.today(), Prefs.libRemKhatm(this)), null);
                closeCreate();
                refresh();
            });
            LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(-2, Ui.ipx(64 + 2 * FocusTile.REACH));
            if (presets.getChildCount() > 0) plp.setMarginStart(Ui.ipx(16 - 2 * FocusTile.REACH));
            presets.addView(p, plp);
        }
        col.addView(presets, lp(22, 0));

        // the custom shape: pages a day (5..40) or juz a day (a quarter..two), start delay, create
        custom = new LinearLayout(this);
        custom.setOrientation(LinearLayout.VERTICAL);
        custom.setClipChildren(false);
        TextView customTitle = text(ui600, 26, Tokens.PLASTER_100);
        customTitle.setText(t.khCustom());
        custom.addView(customTitle, lp(10, 0));

        LinearLayout modes = new LinearLayout(this);
        modes.setOrientation(LinearLayout.HORIZONTAL);
        modes.setClipChildren(false);
        byPages = pill(Icon.BOOK, t.khByPages());
        byPages.setOnClickListener(v -> {
            byJuzMode = false;
            shapeLine();
        });
        byJuz = pill(Icon.RADIO, t.khByJuz());
        byJuz.setOnClickListener(v -> {
            byJuzMode = true;
            shapeLine();
        });
        modes.addView(byPages, pillLp());
        modes.addView(byJuz, pillLp());
        custom.addView(modes, lp(8, 0));

        less = pill(Icon.PREV, "−");
        more = pill(Icon.NEXT, "+");
        less.setOnClickListener(v -> {
            amount--;
            shapeLine();
        });
        more.setOnClickListener(v -> {
            amount++;
            shapeLine();
        });
        shape = text(ui500, 28, Tokens.GOLD_400);
        LinearLayout amountRow = new LinearLayout(this);
        amountRow.setOrientation(LinearLayout.HORIZONTAL);
        amountRow.setGravity(Gravity.CENTER_VERTICAL);
        amountRow.setClipChildren(false);
        amountRow.addView(less, pillLp());
        amountRow.addView(shape, new LinearLayout.LayoutParams(-2, -2));
        amountRow.addView(more, pillLp());
        custom.addView(amountRow, lp(8, 0));

        // the start delay
        delayLess = pill(Icon.PREV, "−");
        delayMore = pill(Icon.NEXT, "+");
        delayLess.setOnClickListener(v -> {
            startDelay--;
            shapeLine();
        });
        delayMore.setOnClickListener(v -> {
            startDelay++;
            shapeLine();
        });
        delay = text(ui400, 24, Tokens.MIST_400);
        LinearLayout delayRow = new LinearLayout(this);
        delayRow.setOrientation(LinearLayout.HORIZONTAL);
        delayRow.setGravity(Gravity.CENTER_VERTICAL);
        delayRow.setClipChildren(false);
        delayRow.addView(delayLess, pillLp());
        delayRow.addView(delay, new LinearLayout.LayoutParams(-2, -2));
        delayRow.addView(delayMore, pillLp());
        custom.addView(delayRow, lp(8, 0));

        FocusTile go = pill(Icon.CHECK, t.khCreate());
        go.setOnClickListener(v -> {
            float perDay = byJuzMode ? amount * 20.13f : amount;
            perDay = Math.max(2, Math.min(120, perDay));
            int d = (int) Math.ceil(Quran.PAGES / perDay);
            final int start = UserDb.dayKeyAfter(UserDb.today(), Math.max(0, startDelay));
            final float pagesPerDay = (float) Quran.PAGES / d;
            Lib.io(() -> UserDb.of(this).createPlan(d, pagesPerDay, start, Prefs.libRemKhatm(this)), null);
            closeCreate();
            refresh();
        });
        custom.addView(go, lp(18, 0));

        col.addView(custom, lp(24, 0));
        panel.addView(col, new FrameLayout.LayoutParams(-1, -1));
        FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(Ui.ipx(900), -2, Gravity.TOP | Gravity.END);
        plp.setMarginEnd(Ui.ipx(96));
        plp.topMargin = Ui.ipx(150);
        root.addView(panel, plp);
        createPanel = panel;
        shapeLine();
        if (presets.getChildCount() > 0) {
            ((FocusTile) presets.getChildAt(0)).requestFocus();
        }
    }

    private FrameLayout createPanel;
    private LinearLayout custom;
    private FocusTile byPages, byJuz, less, more, delayLess, delayMore;
    private TextView shape, delay;
    private boolean byJuzMode = false;
    private int amount = 20;
    private int startDelay = 0;

    /** The custom shape's words: pages or juz a day, and when it starts. */
    private void shapeLine() {
        amount = Math.max(byJuzMode ? 1 : 5, Math.min(byJuzMode ? 10 : 40, amount));
        startDelay = Math.max(0, Math.min(14, startDelay));
        shape.setText(byJuzMode ? t.d(amount + " ج") : t.d(amount + " ص"));
        delay.setText(t.khStartsIn(startDelay));
        byPages.setRest(byJuzMode ? Tokens.LAPIS_900 : Tokens.LAPIS_800, byJuzMode ? Tokens.LINE_SOFT : Tokens.GOLD_400, 2);
        byJuz.setRest(byJuzMode ? Tokens.LAPIS_800 : Tokens.LAPIS_900, byJuzMode ? Tokens.GOLD_400 : Tokens.LINE_SOFT, 2);
    }

    private void closeCreate() {
        if (createPanel == null) return;
        root.removeView(createPanel);
        createPanel = null;
    }

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if (createPanel != null && keyCode == android.view.KeyEvent.KEYCODE_BACK) {
            closeCreate();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    void onTick() {
    }

    // ---------------------------------------------------------------- chrome

    private FocusTile pill(int iconKind, String label) {
        FocusTile b = new FocusTile(this, 34);
        b.setRest(Tokens.LAPIS_900, Tokens.LINE_SOFT, 2);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.ipx(24), 0, Ui.ipx(28), 0);
        Icon icon = new Icon(this, iconKind, 1.8f);
        TextView v = text(ui400, 22, Tokens.PLASTER_100);
        v.setText(label);
        v.setSingleLine(true);
        row.addView(icon, new LinearLayout.LayoutParams(Ui.ipx(24), Ui.ipx(24)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMarginStart(Ui.ipx(10));
        row.addView(v, lp);
        b.addView(row, new FrameLayout.LayoutParams(-2, -1));
        b.setLook(f -> {
            int c = f ? Tokens.LAPIS_900 : Tokens.PLASTER_100;
            v.setTextColor(c);
            icon.setColor(c);
        });
        return b;
    }

    private FocusTile chip(int iconKind, String label) {
        return pill(iconKind, label);
    }

    private LinearLayout.LayoutParams pillLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, Ui.ipx(64 + 2 * FocusTile.REACH));
        lp.setMarginStart(Ui.ipx(16 - 2 * FocusTile.REACH));
        return lp;
    }

    private LinearLayout.LayoutParams lp(int top, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = Ui.ipx(top);
        lp.bottomMargin = Ui.ipx(bottom);
        return lp;
    }
}
