package pro.datascoop.tv;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * The Islamic Library's home (design: a bento grid — one tall card for the Qur'an, three for the adhkar, the
 * continue card, the khatm card spanning two cells, and a row of small status cards that appear only when
 * they have something to say). Everything opens with OK; Back returns Home.
 */
public class LibraryActivity extends Screen {

    // The bento grid, in design px on the 1920×1080 canvas.
    private static final float GRID_X = 680, GRID_W = 1144;
    private static final float ROW1_Y = 190, ROW2_Y = 484, ROW3_Y = 778, ROW_H = 270, STATUS_H = 154;
    private static final float CELL_W = (GRID_W - 2 * 24) / 3f;
    private static final float WIDE_W = GRID_W - CELL_W - 24;
    private static final float STATUS_W = (GRID_W - 4 * 20) / 5f;
    private static final float HERO_W = GRID_X - 96 - 24, HERO_H = ROW_H * 2 + 24 + STATUS_H + 24;

    private TextView heroContinue, heroListen, continueSub, khatmSub;
    private final TextView[] adhkarSubs = new TextView[3];
    private LinearLayout statusRow;
    private LinearLayout hints;

    @Override
    void build() {
        newRoot();

        // ---- header: back, title, library settings
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
        TextView title = text(displayBold, 64, Tokens.PLASTER_100);
        title.setText(t.libTitle());
        lineHeight(title, 1f);
        head.addView(title);
        TextView headSub = text(ui400, 22, Tokens.MIST_400);
        headSub.setText(t.lsAzkarSource());
        head.addView(headSub, margins(4, 0, 0, 0));
        head.setClipChildren(false);
        place(head, 96 + 72 + 28, 56, ViewGroup.LayoutParams.WRAP_CONTENT, 84);

        FocusTile gear = new FocusTile(this, 36);
        gear.setRest(Tokens.LAPIS_800, Tokens.LINE_SOFT, 2);
        Icon gearIcon = new Icon(this, Icon.GEAR, 1.8f);
        gear.addView(gearIcon, new FrameLayout.LayoutParams(Ui.ipx(34), Ui.ipx(34), Gravity.CENTER));
        gear.setLook(f -> gearIcon.setColor(f ? Tokens.LAPIS_900 : Tokens.PLASTER_100));
        gear.setContentDescription(t.settings());
        gear.setOnClickListener(v -> startSafely(new Intent(this, LibrarySettingsActivity.class)));
        placeEnd(gear, 96, 56, 72, 72);

        // ---- the tall Qur'an card
        LinearLayout heroCol = card(HERO_W, HERO_H, Tokens.GOLD_400_60);
        Icon book = new Icon(this, Icon.BOOK, 2.6f);
        book.setColor(Tokens.GOLD_400);
        heroCol.addView(book, new LinearLayout.LayoutParams(Ui.ipx(64), Ui.ipx(64)));
        TextView heroTitle = text(displayBold, 54, Tokens.PLASTER_100);
        heroTitle.setText(t.libQuran());
        heroCol.addView(heroTitle, margins(20, 0, 0, 0));
        TextView heroSub = text(ui400, 24, Tokens.MIST_400);
        heroSub.setText(t.libQuranSub());
        heroSub.setMaxLines(2);
        heroSub.setEllipsize(TextUtils.TruncateAt.END);
        heroCol.addView(heroSub, margins(8, 0, 0, 0));
        heroCol.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1));
        heroContinue = text(ui600, 28, Tokens.GOLD_400);
        heroContinue.setMaxLines(2);
        heroContinue.setEllipsize(TextUtils.TruncateAt.END);
        heroCol.addView(heroContinue, margins(0, 0, 0, 0));
        heroListen = text(ui400, 24, Tokens.MIST_400);
        heroListen.setMaxLines(2);
        heroListen.setEllipsize(TextUtils.TruncateAt.END);
        heroCol.addView(heroListen, margins(10, 0, 0, 0));
        FocusTile heroTile = wrap(heroCol, 96, ROW1_Y, HERO_W, HERO_H, 1.03f);
        heroTile.setOnClickListener(v -> startSafely(new Intent(this, QuranIndexActivity.class)));
        heroTile.setOnFocusChangeListener((v, has) -> { if (has) hint(t.hintOk(t.libQuran()), t.hintBackHome()); });

        // ---- the three adhkar cards
        adhkarCard(0, Icon.STAR, t.libMorning(), GRID_X, ROW1_Y);
        adhkarCard(1, Icon.MOON, t.libEvening(), GRID_X + CELL_W + 24, ROW1_Y);
        adhkarCard(2, Icon.BELL, t.libAfter(), GRID_X + 2 * (CELL_W + 24), ROW1_Y);

        // ---- continue + khatm
        LinearLayout continueCol = card(CELL_W, ROW_H, Tokens.LINE_SOFT);
        fillCard(continueCol, Icon.BOOK, t.libContinue());
        continueSub = sub(continueCol);
        FocusTile contTile = wrap(continueCol, GRID_X, ROW2_Y, CELL_W, ROW_H, 1.04f);
        contTile.setOnClickListener(v -> {
            int[] pos = UserDb.of(this).readPos();
            startSafely(new Intent(this, QuranReaderActivity.class)
                .putExtra("surah", pos[0]).putExtra("ayah", pos[1]));
        });
        contTile.setOnFocusChangeListener((v, has) -> { if (has) hint(t.hintOk(t.libContinue()), t.hintBackHome()); });

        LinearLayout khatmCol = card(WIDE_W, ROW_H, Tokens.LINE_SOFT);
        fillCard(khatmCol, Icon.MIHRAB, t.libKhatm());
        khatmSub = sub(khatmCol);
        khatmSub.setMaxLines(4);
        FocusTile khatmTile = wrap(khatmCol, GRID_X + CELL_W + 24, ROW2_Y, WIDE_W, ROW_H, 1.03f);
        khatmTile.setOnClickListener(v -> startSafely(new Intent(this, KhatmActivity.class)));
        khatmTile.setOnFocusChangeListener((v, has) -> { if (has) hint(t.hintOk(t.libKhatm()), t.hintBackHome()); });

        // ---- the status row: cards that have something to say, filled by refreshData
        statusRow = new LinearLayout(this);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setClipChildren(false);
        place(statusRow, GRID_X, ROW3_Y, GRID_W, STATUS_H);

        hints = new LinearLayout(this);
        hints.setOrientation(LinearLayout.HORIZONTAL);
        place(hints, 96, 962, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        refreshData();
        heroTile.requestFocus();
    }

    @Override
    void onTick() {
    }

    // ---------------------------------------------------------------- card pieces

    private LinearLayout card(float w, float h, int border) {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setPadding(Ui.ipx(28), Ui.ipx(26), Ui.ipx(28), Ui.ipx(26));
        v.setClipChildren(false);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Tokens.LAPIS_800);
        bg.setCornerRadius(Ui.px(Tokens.RADIUS_PANEL));
        bg.setStroke(Ui.ipx(2), border);
        v.setBackground(bg);
        return v;
    }

    /** Puts a card on a focus tile at a design rect and returns the tile. */
    private FocusTile wrap(LinearLayout col, float x, float y, float w, float h, float scale) {
        FocusTile tile = new FocusTile(this, Tokens.RADIUS_PANEL);
        tile.setRest(0, 0, 0);
        tile.addView(col, new FrameLayout.LayoutParams(-1, -1));
        tile.setFocusScale(scale);
        placeTile(tile, x, y, w, h);
        return tile;
    }

    private LinearLayout.LayoutParams margins(int top, int start, int end, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = Ui.ipx(top);
        lp.setMarginStart(Ui.ipx(start));
        lp.setMarginEnd(Ui.ipx(end));
        lp.bottomMargin = Ui.ipx(bottom);
        return lp;
    }

    private TextView sub(LinearLayout col) {
        TextView v = text(ui400, 22, Tokens.MIST_400);
        v.setMaxLines(3);
        v.setEllipsize(TextUtils.TruncateAt.END);
        col.addView(v, margins(10, 0, 0, 0));
        return v;
    }

    /** An adhkar card: icon, title, sub (count, then today's progress). */
    private void adhkarCard(int section, int iconKind, String title, float x, float y) {
        LinearLayout col = card(CELL_W, ROW_H, Tokens.LINE_SOFT);
        Icon icon = new Icon(this, iconKind, 2f);
        icon.setColor(Tokens.GOLD_400);
        col.addView(icon, new LinearLayout.LayoutParams(Ui.ipx(44), Ui.ipx(44)));
        TextView label = text(displayBold, 36, Tokens.PLASTER_100);
        label.setText(title);
        label.setMaxLines(2);
        label.setEllipsize(TextUtils.TruncateAt.END);
        col.addView(label, margins(14, 0, 0, 0));
        adhkarSubs[section] = sub(col);
        FocusTile tile = wrap(col, x, y, CELL_W, ROW_H, 1.04f);
        tile.setOnClickListener(v -> startSafely(new Intent(this, AdhkarActivity.class).putExtra("section", section)));
        tile.setOnFocusChangeListener((v, has) -> { if (has) hint(t.hintOk(title), t.hintBackHome()); });
    }

    /** A card with icon + title, ready for its sub line. */
    private TextView fillCard(LinearLayout col, int iconKind, String title) {
        Icon icon = new Icon(this, iconKind, 2f);
        icon.setColor(Tokens.GOLD_400);
        col.addView(icon, new LinearLayout.LayoutParams(Ui.ipx(44), Ui.ipx(44)));
        TextView label = text(displayBold, 36, Tokens.PLASTER_100);
        label.setText(title);
        label.setMaxLines(2);
        label.setEllipsize(TextUtils.TruncateAt.END);
        col.addView(label, margins(14, 0, 0, 0));
        return label;
    }

    // ---------------------------------------------------------------- live data

    /** Fills the sub lines and the status row from the databases, off the main thread. */
    private void refreshData() {
        final LibraryActivity self = this;
        Lib.io(() -> {
            Azkar az = Azkar.get(this);
            final int nM = az.section(Azkar.MORNING).length;
            final int nE = az.section(Azkar.EVENING).length;
            final int nP = az.section(Azkar.AFTER_PRAYER).length;
            int doneM = 0, doneE = 0, doneP = 0;
            for (int c : UserDb.of(this).azkarProgress("m", UserDb.today(), nM)) if (c > 0) doneM++;
            for (int c : UserDb.of(this).azkarProgress("e", UserDb.today(), nE)) if (c > 0) doneE++;
            for (int c : UserDb.of(this).azkarProgress("p", UserDb.today(), nP)) if (c > 0) doneP++;
            final int dM = doneM, dE = doneE, dP = doneP;
            final int[] pos = UserDb.of(this).readPos();
            final boolean hasPos = UserDb.of(this).hasReadPos();
            final int[] last = UserDb.of(this).lastListened(Prefs.reciter(this));
            final List<UserDb.KhatmPlan> plans = UserDb.of(this).plans();
            final int pages = plans.isEmpty() ? 0 : UserDb.of(this).pagesRead(plans.get(0).id);
            final UserDb.KhatmPlan plan = plans.isEmpty() ? null : plans.get(0);
            final Reciters.Reciter r = Reciters.get(this).of(Prefs.reciter(this));
            Lib.main(() -> {
                if (self.isFinishing() || adhkarSubs[0] == null) return;
                adhkarSubs[0].setText(t.adhkarCount(nM) + (dM > 0 ? t.sep() + t.adhkarProgress(dM, nM) : ""));
                adhkarSubs[1].setText(t.adhkarCount(nE) + (dE > 0 ? t.sep() + t.adhkarProgress(dE, nE) : ""));
                adhkarSubs[2].setText(t.adhkarCount(nP) + (dP > 0 ? t.sep() + t.adhkarProgress(dP, nP) : ""));
                if (plan != null) {
                    khatmSub.setText(t.khDays(plan.days) + t.sep() + t.khProgress((int) Math.round(pages * 100.0 / Quran.PAGES))
                        + t.sep() + t.khOfPages(pages, Quran.PAGES));
                } else {
                    khatmSub.setText(t.khNone());
                }
                if (hasPos) {
                    Quran.ready(self, () -> {
                        Quran q = Quran.opened();
                        if (q == null || self.isFinishing()) return;
                        Quran.Surah s = q.surah(pos[0]);
                        String where = (t.ar ? s.name : s.nameEn) + t.sep() + t.qrAyahMark(pos[1]);
                        if (heroContinue != null) heroContinue.setText(t.libContinue() + t.sep() + where);
                        if (continueSub != null) continueSub.setText(where);
                        buildStatus(pos, hasPos, last, plan, pages, r);
                    });
                } else {
                    if (heroContinue != null) heroContinue.setText(t.libQuran());
                    if (continueSub != null) continueSub.setText(t.libQuranSub());
                    buildStatus(pos, false, last, plan, pages, r);
                }
                if (last != null && r != null) {
                    Quran.ready(self, () -> {
                        Quran q = Quran.opened();
                        if (q == null || self.isFinishing()) return;
                        Quran.Surah s = q.surah(last[0]);
                        if (heroListen != null) heroListen.setText(t.libListen() + t.sep() + r.name(t) + t.sep() + (t.ar ? s.name : s.nameEn));
                    });
                } else if (heroListen != null) {
                    heroListen.setText(r == null ? "" : t.rcRewaya());
                }
            });
        });
    }

    /** The small status cards, in their order: last read, reciter, khatm, today, reminders. */
    private void buildStatus(int[] pos, boolean hasPos, int[] last, UserDb.KhatmPlan plan, int pages, Reciters.Reciter r) {
        statusRow.removeAllViews();
        int i = 0;
        if (hasPos) {
            final int surah = pos[0], ayah = pos[1];
            statusCard(t.libStatusSurah(), t.sep(),
                v -> startSafely(new Intent(this, QuranReaderActivity.class).putExtra("surah", surah).putExtra("ayah", ayah)), i++);
            Quran.ready(this, () -> {
                Quran q = Quran.opened();
                if (q == null || statusRow.getChildCount() == 0 || isFinishing()) return;
                Quran.Surah s = q.surah(surah);
                TextView value = valueOf(0);
                if (value != null) value.setText((t.ar ? s.name : s.nameEn) + " " + t.qrAyahMark(ayah));
            });
        }
        if (r != null) {
            statusCard(t.libStatusReciter(), r.name(t), v -> startSafely(new Intent(this, RecitersActivity.class)), i++);
        }
        if (plan != null) {
            statusCard(t.libStatusKhatm(), t.khProgress((int) Math.round(pages * 100.0 / Quran.PAGES)),
                v -> startSafely(new Intent(this, KhatmActivity.class)), i++);
            statusCard(t.libStatusToday(), t.khPagesDaily((int) Math.round(plan.pagesPerDay)),
                v -> startSafely(new Intent(this, KhatmActivity.class)), i++);
        }
        statusCard(t.libStatusRemind(), Prefs.anyLibReminder(this) ? t.libOn() : t.libOff(),
            v -> startSafely(new Intent(this, LibrarySettingsActivity.class)), i);
    }

    /** A small status card: caps label over a value; it opens somewhere. Returns the tile. */
    private FocusTile statusCard(String label, String value, View.OnClickListener open, int i) {
        FocusTile tile = new FocusTile(this, Tokens.RADIUS_CELL);
        tile.setRest(Tokens.LAPIS_800, Tokens.LINE_SOFT, 2);
        tile.setFocusScale(1.05f);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.ipx(20), Ui.ipx(18), Ui.ipx(20), Ui.ipx(18));
        TextView l = caps(text(ui400, 18, Tokens.MIST_400));
        l.setText(label);
        l.setMaxLines(1);
        l.setEllipsize(TextUtils.TruncateAt.END);
        TextView v = text(ui500, 24, Tokens.PLASTER_100);
        v.setText(value);
        v.setMaxLines(2);
        v.setEllipsize(TextUtils.TruncateAt.END);
        col.addView(l);
        col.addView(v, margins(8, 0, 0, 0));
        tile.addView(col, new FrameLayout.LayoutParams(-1, -1));
        tile.setLook(f -> {
            v.setTextColor(f ? Tokens.LAPIS_900 : Tokens.PLASTER_100);
            l.setTextColor(f ? Tokens.LAPIS_900 : Tokens.MIST_400);
        });
        tile.setOnClickListener(open);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.ipx(STATUS_W), Ui.ipx(STATUS_H));
        if (i > 0) lp.setMarginStart(Ui.ipx(20));
        statusRow.addView(tile, lp);
        return tile;
    }

    /** The value TextView of the i-th status card, or null when the card is gone. */
    private TextView valueOf(int i) {
        if (i >= statusRow.getChildCount()) return null;
        FocusTile tile = (FocusTile) statusRow.getChildAt(i);
        LinearLayout col = (LinearLayout) tile.getChildAt(0);
        return col.getChildCount() > 1 ? (TextView) col.getChildAt(1) : null;
    }

    // ---------------------------------------------------------------- helpers

    private void hint(String ok, String back) {
        List<String> h = new ArrayList<>();
        h.add(ok);
        h.add(back);
        setHints(hints, h);
    }
}
