package pro.datascoop.tv;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
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
import java.util.Locale;

/**
 * The Qur'an index: all 114 surahs on a long grid, the search panel (by number or by name — an on-screen digit
 * pad and letter grid, because TVs type with the remote), the day's continue chip when reading has begun, and
 * the bookmark list. Opening a surah goes to the reader, straight to where reading last stopped.
 */
public class QuranIndexActivity extends Screen {

    private static final int COLS = 6, ROW_H = 200, GAP = 24, GRID_TOP = 200;
    private static final float TILE_W = (1728 - (COLS - 1) * GAP) / COLS;

    private ScrollView grid;
    private LinearLayout rows;
    private LinearLayout hints;
    private FocusTile searchChip;
    /** The search panel while open; null while closed. */
    private FrameLayout panel;
    private TextView typed, matchCount;
    private LinearLayout matches;
    private boolean byName = true;
    private final StringBuilder buf = new StringBuilder();
    private Quran.Surah[] surahs;
    private final List<FocusTile> tiles = new ArrayList<>();

    @Override
    void build() {
        newRoot();
        surahs = null;
        tiles.clear();
        Quran.ready(this, () -> {
            surahs = Quran.opened().surahs();
            if (!isFinishing() && rows != null) fillGrid();
        });

        // ---- header
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
        title.setText(t.libQuran());
        lineHeight(title, 1f);
        head.addView(title);
        TextView sub = text(ui400, 22, Tokens.MIST_400);
        sub.setText(t.libQuranSub());
        head.addView(sub, lp(4, 0));
        head.setClipChildren(false);
        place(head, 96 + 72 + 28, 56, ViewGroup.LayoutParams.WRAP_CONTENT, 84);

        searchChip = chip(Icon.SEARCH, t.qiSearch());
        searchChip.setOnClickListener(v -> openPanel());

        // ---- the continue chip (only once reading has begun), beside the search chip
        UserDb db = UserDb.of(this);
        if (db.hasReadPos()) {
            int[] pos = db.readPos();
            FocusTile cont = chip(Icon.BOOKMARK, t.libContinue());
            cont.setOnClickListener(v -> startSafely(new Intent(this, QuranReaderActivity.class)
                .putExtra("surah", pos[0]).putExtra("ayah", pos[1])));
            LinearLayout pills = new LinearLayout(this);
            pills.setOrientation(LinearLayout.HORIZONTAL);
            pills.setClipChildren(false);
            float r = FocusTile.REACH;
            pills.addView(cont, new LinearLayout.LayoutParams(-2, Ui.ipx(72 + 2 * r)));
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-2, Ui.ipx(72 + 2 * r));
            slp.setMarginStart(Ui.ipx(20 - 2 * r));
            pills.addView(searchChip, slp);
            placeEnd(pills, 96 + 360, 56, 760, 72 + 2 * r);
        } else {
            placeEnd(searchChip, 96, 56, ViewGroup.LayoutParams.WRAP_CONTENT, 72);
        }

        // ---- the grid
        grid = new ScrollView(this);
        grid.setFillViewport(true);
        grid.setVerticalScrollBarEnabled(false);
        rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setPadding(Ui.ipx(0), Ui.ipx(4), Ui.ipx(0), Ui.ipx(48));
        rows.setClipChildren(false);
        grid.addView(rows, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        place(grid, 96, GRID_TOP, 1728, 940 - GRID_TOP);
        fillGrid();

        hints = new LinearLayout(this);
        hints.setOrientation(LinearLayout.HORIZONTAL);
        place(hints, 96, 962, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setHints(hints, hints2());
    }

    @Override
    void onTick() {
    }

    private List<String> hints2() {
        List<String> h = new ArrayList<>();
        h.add(t.hintQi());
        h.add(t.hintBackHome());
        return h;
    }

    /** The 114 surah tiles, a row at a time. */
    private void fillGrid() {
        rows.removeAllViews();
        for (int i = 0; i < surahs.length; i += COLS) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setClipChildren(false);
            for (int j = 0; j < COLS && i + j < surahs.length; j++) {
                FocusTile tile = surahTile(surahs[i + j]);
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(Ui.ipx(TILE_W), Ui.ipx(ROW_H));
                if (j > 0) rlp.setMarginStart(Ui.ipx(GAP));
                row.addView(tile, rlp);
            }
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, -2);
            if (rows.getChildCount() > 0) rlp.topMargin = Ui.ipx(GAP);
            rows.addView(row, rlp);
        }
        if (!tiles.isEmpty()) tiles.get(0).requestFocus();
    }

    private FocusTile surahTile(Quran.Surah s) {
        FocusTile tile = new FocusTile(this, Tokens.RADIUS_TILE);
        tile.setRest(Tokens.LAPIS_800, Tokens.LINE_SOFT, 2);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.ipx(24), Ui.ipx(20), Ui.ipx(24), Ui.ipx(18));
        TextView number = text(display, 26, Tokens.GOLD_400);
        number.setTextDirection(View.TEXT_DIRECTION_LTR);
        number.setText(t.d(String.valueOf(s.number)));
        TextView name = text(displayBold, 34, Tokens.PLASTER_100);
        name.setText(t.ar ? s.name : s.nameEn);
        name.setMaxLines(2);
        name.setEllipsize(TextUtils.TruncateAt.END);
        name.setTextDirection(View.TEXT_DIRECTION_RTL);
        TextView info = text(ui400, 20, Tokens.MIST_400);
        info.setText((s.meccan ? t.qiMeccan() : t.qiMedinan()) + t.sep() + t.qiAyahs(s.ayahs));
        info.setMaxLines(1);
        info.setEllipsize(TextUtils.TruncateAt.END);
        col.addView(number);
        col.addView(name, lp(4, 0));
        col.addView(info, lp(6, 0));
        tile.addView(col, new FrameLayout.LayoutParams(-1, -1));
        tile.setLook(f -> {
            name.setTextColor(f ? Tokens.LAPIS_900 : Tokens.PLASTER_100);
            number.setTextColor(f ? Tokens.LAPIS_900 : Tokens.GOLD_400);
            info.setTextColor(f ? Tokens.LAPIS_900 : Tokens.MIST_400);
        });
        tile.setOnClickListener(v -> openSurah(s));
        tile.setContentDescription((t.ar ? s.name : s.nameEn));
        tiles.add(tile);
        return tile;
    }

    private void openSurah(Quran.Surah s) {
        startSafely(new Intent(this, QuranReaderActivity.class).putExtra("surah", s.number));
    }

    // ---------------------------------------------------------------- the search panel

    /** The panel: a mode chip (number or name), the pad, what is typed, and the live matches. */
    private void openPanel() {
        closePanel();
        buf.setLength(0);
        byName = true;

        panel = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Tokens.LAPIS_700);
        bg.setCornerRadius(Ui.px(Tokens.RADIUS_PANEL));
        bg.setStroke(Ui.ipx(2), Tokens.GOLD_400_60);
        panel.setBackground(bg);
        panel.setPadding(Ui.ipx(36), Ui.ipx(30), Ui.ipx(36), Ui.ipx(30));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setClipChildren(false);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        FocusTile mode = chip(Icon.SEARCH, byName ? t.qiByName() : t.qiByNumber());
        mode.setOnClickListener(v -> {
            byName = !byName;
            buf.setLength(0);
            typed.setText("");
            apply();
        });
        modeChipRef = mode;
        top.addView(mode, new LinearLayout.LayoutParams(-2, Ui.ipx(64 + 2 * FocusTile.REACH)));
        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        typed = text(display, 44, Tokens.GOLD_400);
        typed.setMinWidth(Ui.ipx(240));
        typed.setTextDirection(View.TEXT_DIRECTION_RTL);
        matchCount = text(ui400, 20, Tokens.MIST_400);
        right.addView(typed);
        right.addView(matchCount, lp(4, 0));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(0, -2, 1);
        rlp.setMarginStart(Ui.ipx(32));
        top.addView(right, rlp);
        col.addView(top);

        pad = new LinearLayout(this);
        col.addView(pad, lp(20, 0));
        buildPad();

        matches = new LinearLayout(this);
        matches.setOrientation(LinearLayout.VERTICAL);
        matches.setClipChildren(false);
        col.addView(matches, lp(20, 0));

        panel.addView(col, new FrameLayout.LayoutParams(-1, -1));
        FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(Ui.ipx(920), -2, Gravity.TOP | Gravity.END);
        plp.setMarginEnd(Ui.ipx(96));
        plp.topMargin = Ui.ipx(160);
        root.addView(panel, plp);
        for (FocusTile tile : tiles) tile.setFocusable(false);
        apply();
        if (!padKeys.isEmpty()) padKeys.get(0).requestFocus();
    }

    private FocusTile modeChipRef;
    private LinearLayout pad;
    private final List<FocusTile> padKeys = new ArrayList<>();

    /** The pad itself: the Arabic (or Latin) letters, or the digits. */
    private void buildPad() {
        pad.removeAllViews();
        padKeys.clear();
        String keys = byName
            ? (t.ar || t.lang == Text.UR ? "ابتثجحخدذرزسشصضطظعغفقكلمنهوي" : "abcdefghijklmnopqrstuvwxyz")
            : "123456789";
        int cols = byName ? 8 : 5;
        LinearLayout row = null;
        for (int i = 0; i < keys.length(); i++) {
            if (i % cols == 0) {
                row = new LinearLayout(this);
                row.setClipChildren(false);
                pad.addView(row, lp(0, 6));
            }
            row.addView(keyTile(String.valueOf(keys.charAt(i))), keyLp());
        }
        // ⌫ always at the end of the last row
        if (row != null) {
            LinearLayout.LayoutParams blp = keyLp();
            blp.setMarginStart(Ui.ipx(12));
            row.addView(keyTile("⌫"), blp);
        }
    }

    private LinearLayout.LayoutParams keyLp() {
        LinearLayout.LayoutParams klp = new LinearLayout.LayoutParams(Ui.ipx(88), Ui.ipx(56));
        return klp;
    }

    private FocusTile keyTile(String label) {
        FocusTile b = new FocusTile(this, Tokens.RADIUS_CELL);
        b.setRest(Tokens.LAPIS_900, Tokens.LINE_SOFT, 2);
        TextView v = text(ui500, 26, Tokens.PLASTER_100);
        v.setText(label);
        v.setGravity(Gravity.CENTER);
        b.addView(v, new FrameLayout.LayoutParams(-1, -1));
        b.setLook(f -> v.setTextColor(f ? Tokens.LAPIS_900 : Tokens.PLASTER_100));
        b.setOnClickListener(x -> {
            if ("⌫".equals(label)) {
                if (buf.length() > 0) buf.setLength(buf.length() - 1);
            } else if (buf.length() < 12) {
                buf.append(label);
            }
            typed.setText(buf.toString());
            apply();
        });
        padKeys.add(b);
        return b;
    }

    /** The matches under what is typed, live. */
    private void apply() {
        if (modeChipRef != null) {
            ((TextView) ((LinearLayout) modeChipRef.getChildAt(0)).getChildAt(1)).setText(byName ? t.qiByName() : t.qiByNumber());
        }
        matches.removeAllViews();
        List<Quran.Surah> found = new ArrayList<>();
        String q = buf.toString().trim();
        if (!q.isEmpty() && surahs != null) {
            if (byName) {
                String plain = Quran.normalize(q);
                for (Quran.Surah s : surahs) {
                    String hay = t.ar || t.lang == Text.UR ? s.plain : s.nameEn.toLowerCase(Locale.US);
                    if (hay.startsWith(plain)) found.add(s);
                }
            } else {
                try {
                    int n = Integer.parseInt(q);
                    for (Quran.Surah s : surahs) if (s.number == n) found.add(s);
                    if (found.isEmpty() && n >= 1 && n <= Quran.SURAHS) found.add(surahs[n - 1]);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        int shown = Math.min(found.size(), 6);
        for (int i = 0; i < shown; i++) {
            FocusTile tile = new FocusTile(this, Tokens.RADIUS_ROW);
            tile.setRest(Tokens.LAPIS_900, Tokens.LINE_SOFT, 2);
            tile.setFocusScale(Tokens.FOCUS_SCALE_ROW);
            LinearLayout r = new LinearLayout(this);
            r.setOrientation(LinearLayout.HORIZONTAL);
            r.setGravity(Gravity.CENTER_VERTICAL);
            r.setPadding(Ui.ipx(24), 0, Ui.ipx(24), 0);
            final Quran.Surah s = found.get(i);
            TextView num = text(display, 24, Tokens.GOLD_400);
            num.setTextDirection(View.TEXT_DIRECTION_LTR);
            num.setText(t.d(String.valueOf(s.number)));
            TextView nm = text(displayBold, 30, Tokens.PLASTER_100);
            nm.setText(t.ar ? s.name : s.nameEn);
            LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(-2, -2);
            nlp.setMarginStart(Ui.ipx(20));
            r.addView(num);
            r.addView(nm, nlp);
            tile.addView(r, new FrameLayout.LayoutParams(-1, -1));
            tile.setLook(f -> {
                nm.setTextColor(f ? Tokens.LAPIS_900 : Tokens.PLASTER_100);
                num.setTextColor(f ? Tokens.LAPIS_900 : Tokens.GOLD_400);
            });
            tile.setOnClickListener(v -> {
                closePanel();
                openSurah(s);
            });
            matches.addView(tile, lp(0, 10));
        }
        matchCount.setText(q.isEmpty() ? t.qiSearch() : found.isEmpty() ? t.qiNoResults() : t.qiResults(found.size()));
    }

    private void closePanel() {
        if (panel == null) return;
        root.removeView(panel);
        panel = null;
        for (FocusTile tile : tiles) tile.setFocusable(true);
    }

    // ---------------------------------------------------------------- keys

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (panel != null && e.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (e.getAction() == KeyEvent.ACTION_UP) closePanel();
            return true;
        }
        return super.dispatchKeyEvent(e);
    }

    private LinearLayout.LayoutParams lp(int top, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = Ui.ipx(top);
        lp.bottomMargin = Ui.ipx(bottom);
        return lp;
    }

    private FocusTile chip(int iconKind, String label) {
        FocusTile b = new FocusTile(this, 36);
        b.setRest(Tokens.LAPIS_800, Tokens.LINE_SOFT, 2);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.ipx(28), 0, Ui.ipx(32), 0);
        Icon icon = new Icon(this, iconKind, 1.8f);
        TextView v = text(ui400, 24, Tokens.PLASTER_100);
        v.setText(label);
        v.setSingleLine(true);
        row.addView(icon, new LinearLayout.LayoutParams(Ui.ipx(26), Ui.ipx(26)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMarginStart(Ui.ipx(12));
        row.addView(v, lp);
        b.addView(row, new FrameLayout.LayoutParams(-2, -1));
        b.setLook(f -> {
            int c = f ? Tokens.LAPIS_900 : Tokens.PLASTER_100;
            v.setTextColor(c);
            icon.setColor(c);
        });
        return b;
    }
}
