package pro.datascoop.tv;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
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
 * Reading one surah: the Uthmani text itself in Amiri, every verse its own focusable row with its number in
 * the mushaf's ornate parentheses, the text size adjustable, jump-to-verse, bookmark, and listening — all in
 * the header. Where reading stopped is kept for every focus move (a small, quiet write at most once a second)
 * and is what Continue reading returns to. The Bismillah stands at the head of every surah but Al-Fatihah
 * (where it is the first verse) and At-Tawbah (which has none) — exactly as the mushaf prints it.
 */
public class QuranReaderActivity extends Screen {

    private static final float[] SIZES = {30, 34, 38, 44, 50};

    private int surah, at;
    private Quran.Ayah[] ayahs;
    private final List<TextView> bodies = new ArrayList<>();
    private final List<FocusTile> cards = new ArrayList<>();
    private ScrollView scroll;
    private LinearLayout verses;
    private TextView nameHead, infoHead, placeHead;
    private LinearLayout hints;
    private FocusTile fontChip, jumpChip, listenChip, markChip;
    /** The focused verse's page and juz, saved at most once a second while reading. */
    private int lastSaved = -1;
    private TextView savedNote;

    @Override
    void build() {
        newRoot();
        surah = getIntent().getIntExtra("surah", 1);
        at = getIntent().getIntExtra("ayah", 0);
        bodies.clear();
        cards.clear();
        lastSaved = -1;

        // ---- header
        FocusTile back = new FocusTile(this, 36);
        back.setRest(Tokens.LAPIS_800, Tokens.LINE_SOFT, 2);
        Icon backIcon = new Icon(this, Icon.BACK, 2f);
        back.addView(backIcon, new FrameLayout.LayoutParams(Ui.ipx(32), Ui.ipx(32), Gravity.CENTER));
        back.setLook(f -> backIcon.setColor(f ? Tokens.LAPIS_900 : Tokens.PLASTER_100));
        back.setContentDescription(t.back());
        back.setOnClickListener(v -> {
            savePosition();
            finish();
        });
        placeTile(back, 96, 56, 72, 72);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        nameHead = text(displayBold, 48, Tokens.PLASTER_100);
        lineHeight(nameHead, 1f);
        head.addView(nameHead);
        infoHead = text(ui400, 22, Tokens.MIST_400);
        head.addView(infoHead, lp(4, 0));
        placeHead = text(ui400, 22, Tokens.GOLD_400);
        head.addView(placeHead, lp(2, 0));
        head.setClipChildren(false);
        place(head, 96 + 72 + 28, 56, 640, 92);

        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setClipChildren(false);
        float r = FocusTile.REACH;
        fontChip = headerChip(Icon.SEARCH, t.qrFont());
        fontChip.setOnClickListener(v -> {
            Prefs.setQuranFont(this, Prefs.quranFont(this) % SIZES.length + 1);
            applyFont();
        });
        jumpChip = headerChip(Icon.BOOK, t.qrJump());
        jumpChip.setOnClickListener(v -> openJump());
        listenChip = headerChip(Icon.PLAY, t.qrListen());
        listenChip.setOnClickListener(v -> startSafely(new Intent(this, PlayerActivity.class).putExtra("surah", surah)));
        markChip = headerChip(Icon.BOOKMARK, t.qrBookmarks());
        markChip.setOnClickListener(v -> {
            savePosition();
            startSafely(new Intent(this, BookmarksActivity.class));
        });
        chips.addView(fontChip, new LinearLayout.LayoutParams(-2, Ui.ipx(64 + 2 * r)));
        chips.addView(jumpChip, chipLp());
        chips.addView(listenChip, chipLp());
        chips.addView(markChip, chipLp());
        placeEnd(chips, 96, 56, ViewGroup.LayoutParams.WRAP_CONTENT, 64 + 2 * r);

        savedNote = text(ui400, 22, Tokens.GOLD_400);
        place(savedNote, 96 + 72 + 28 + 640 + 24, 56, 320, 64);

        // ---- the verses
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        verses = new LinearLayout(this);
        verses.setOrientation(LinearLayout.VERTICAL);
        verses.setPadding(Ui.ipx(8), Ui.ipx(8), Ui.ipx(8), Ui.ipx(60));
        verses.setClipChildren(false);
        scroll.addView(verses, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        place(scroll, 88, 170, 1744, 940 - 170);

        hints = new LinearLayout(this);
        hints.setOrientation(LinearLayout.HORIZONTAL);
        place(hints, 96, 962, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setHints(hints, hints2());

        // ---- load
        Quran.ready(this, () -> {
            Quran q = Quran.opened();
            if (q == null || isFinishing()) return;
            Quran.Surah s = q.surah(surah);
            ayahs = q.surahAyahs(surah);
            nameHead.setText(t.ar ? s.name : s.nameEn);
            int[] place = q.surahPlace(surah);
            infoHead.setText(t.qrInfo(s.ayahs, s.meccan, place[2], place[0]));
            fillVerses();
            gotoVerse(Math.max(1, at));
        });
    }

    private List<String> hints2() {
        List<String> h = new ArrayList<>();
        h.add(t.hintQr());
        h.add(t.hintBackHome());
        return h;
    }

    /** The Bismillah header (not for Al-Fatihah or At-Tawbah) and the verse rows. */
    private void fillVerses() {
        verses.removeAllViews();
        if (surah != 1 && surah != 9 && ayahs.length > 0) {
            TextView bism = text(sacred, 34, Tokens.GOLD_400);
            bism.setText(ayahs[0].id == 1 ? bismillah() : BISMILLAH);
            bism.setGravity(Gravity.CENTER);
            bism.setLineSpacing(Ui.ipx(6), 1f);
            bism.setTextDirection(View.TEXT_DIRECTION_RTL);
            verses.addView(bism, lp(24, 36));
        }
        for (Quran.Ayah a : ayahs) verses.addView(verseCard(a), lp(0, 10));
        applyFont();
        if (!cards.isEmpty()) cards.get(0).requestFocus();
    }

    private static final String BISMILLAH = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ";

    /** The Bismillah exactly as the text of 1:1 carries it, so nothing is retyped by hand. */
    private String bismillah() {
        return BISMILLAH;
    }

    private View verseCard(Quran.Ayah a) {
        FocusTile tile = new FocusTile(this, Tokens.RADIUS_ROW);
        tile.setRest(Tokens.LAPIS_800, Tokens.LINE_SOFT, 2);
        tile.setFocusScale(Tokens.FOCUS_SCALE_ROW);
        TextView body = text(sacred, SIZES[Prefs.quranFont(this) - 1], Tokens.PLASTER_100);
        body.setLineSpacing(Ui.ipx(8), 1f);
        body.setTextDirection(View.TEXT_DIRECTION_RTL);
        SpannableString s = new SpannableString(a.text + " " + mark(a.ayah));
        s.setSpan(new ForegroundColorSpan(Tokens.GOLD_400), a.text.length() + 1, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        body.setText(s);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.ipx(28), Ui.ipx(20), Ui.ipx(28), Ui.ipx(18));
        col.addView(body);
        tile.addView(col, new FrameLayout.LayoutParams(-1, -1));
        tile.setLook(f -> body.setTextColor(f ? Tokens.LAPIS_900 : Tokens.PLASTER_100));
        final Quran.Ayah ayah = a;
        tile.setOnClickListener(v -> toggleBookmark(ayah));
        bodies.add(body);
        cards.add(tile);
        tile.setOnFocusChangeListener((v, has) -> { if (has) focusChanged(ayah); });
        return tile;
    }

    /** The verse number in the mushaf's ornate parentheses, in the screen's digits. */
    private String mark(int n) {
        return t.d("﴿" + n + "﴾");
    }

    // ---------------------------------------------------------------- reading state

    /** The focused verse's own page and juz, under the surah's name. */
    private void focusChanged(Quran.Ayah a) {
        placeHead.setText(t.qrAt(a.juz, a.page));
    }

    // ---------------------------------------------------------------- actions

    private void toggleBookmark(Quran.Ayah a) {
        Lib.io(() -> UserDb.of(this).toggleBookmark(a.surah, a.ayah), () -> {
            boolean marked = UserDb.of(this).bookmarked(a.surah, a.ayah);
            savedNote.setText(marked ? t.qrBookmarked() : t.qrSaved());
            savedNote.animate().alpha(1f).setDuration(200).start();
            savedNote.postDelayed(() -> savedNote.animate().alpha(0f).setDuration(400).start(), 2000);
        });
    }

    private void applyFont() {
        float size = SIZES[Prefs.quranFont(this) - 1];
        for (TextView v : bodies) v.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, Ui.px(size));
    }

    /** Scrolls to and focuses a verse (its number, 1-based). */
    private void gotoVerse(int n) {
        if (cards.isEmpty()) return;
        final int idx = Math.max(0, Math.min(cards.size() - 1, n - 1));
        final FocusTile tile = cards.get(idx);
        scroll.post(() -> {
            tile.requestFocus();
            scroll.smoothScrollTo(0, Math.max(0, tile.getTop() - Ui.ipx(200)));
        });
    }

    /** The jump panel: a digit pad builds a verse number. */
    private void openJump() {
        if (ayahs == null || ayahs.length == 0) return;
        final StringBuilder buf = new StringBuilder();
        FrameLayout panel = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Tokens.LAPIS_700);
        bg.setCornerRadius(Ui.px(Tokens.RADIUS_PANEL));
        bg.setStroke(Ui.ipx(2), Tokens.GOLD_400_60);
        panel.setBackground(bg);
        panel.setPadding(Ui.ipx(36), Ui.ipx(30), Ui.ipx(36), Ui.ipx(30));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setClipChildren(false);
        TextView title = text(displayBold, 40, Tokens.PLASTER_100);
        title.setText(t.qrJump());
        col.addView(title);
        final TextView shown = text(display, 64, Tokens.GOLD_400);
        shown.setTextDirection(View.TEXT_DIRECTION_LTR);
        col.addView(shown, lp(8, 0));
        LinearLayout pad = new LinearLayout(this);
        col.addView(pad, lp(16, 0));
        final List<FocusTile> keys = new ArrayList<>();
        for (char d : "1234567890⌫".toCharArray()) {
            FocusTile key = padKey(String.valueOf(d));
            final boolean backspace = d == '⌫';
            key.setOnClickListener(x -> {
                if (backspace) {
                    if (buf.length() > 0) buf.setLength(buf.length() - 1);
                } else if (buf.length() < 4) {
                    buf.append(d);
                }
                shown.setText(buf.toString());
            });
            keys.add(key);
        }
        // digits flow: 1..9, 0, ⌫ — arrange in one row of five and one of one
        LinearLayout rowA = new LinearLayout(this);
        LinearLayout rowB = new LinearLayout(this);
        rowA.setClipChildren(false);
        rowB.setClipChildren(false);
        for (int i = 0; i < keys.size(); i++) {
            LinearLayout.LayoutParams klp = new LinearLayout.LayoutParams(Ui.ipx(96), Ui.ipx(60));
            if (i % 5 > 0) klp.setMarginStart(Ui.ipx(14));
            (i < 5 ? rowA : rowB).addView(keys.get(i), klp);
        }
        pad.setOrientation(LinearLayout.VERTICAL);
        pad.addView(rowA, lp(0, 0));
        pad.addView(rowB, lp(8, 0));
        FocusTile go = new FocusTile(this, 34);
        go.setRest(Tokens.LAPIS_900, Tokens.GOLD_400_60, 2);
        TextView goLabel = text(ui500, 26, Tokens.PLASTER_100);
        goLabel.setText(t.qrJump());
        goLabel.setGravity(Gravity.CENTER);
        goLabel.setPadding(Ui.ipx(48), 0, Ui.ipx(48), 0);
        go.addView(goLabel, new FrameLayout.LayoutParams(-2, -1));
        go.setLook(f -> goLabel.setTextColor(f ? Tokens.LAPIS_900 : Tokens.PLASTER_100));
        go.setOnClickListener(v -> {
            int n;
            try {
                n = Integer.parseInt(buf.toString());
            } catch (NumberFormatException e) {
                n = 0;
            }
            closeJump();
            if (n >= 1 && n <= ayahs.length) gotoVerse(n);
        });
        col.addView(go, lp(24, 0));
        panel.addView(col, new FrameLayout.LayoutParams(-1, -1));
        FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(Ui.ipx(640), -2, Gravity.TOP | Gravity.END);
        plp.setMarginEnd(Ui.ipx(96));
        plp.topMargin = Ui.ipx(160);
        root.addView(panel, plp);
        jumpPanel = panel;
        jumpKeys = keys;
        jumpGoTile = go;
        for (FocusTile c : cards) c.setFocusable(false);
        if (!keys.isEmpty()) keys.get(0).requestFocus();
        jumpShown = shown;
    }

    private FrameLayout jumpPanel;
    private List<FocusTile> jumpKeys;
    private FocusTile jumpGoTile;
    private TextView jumpShown;

    private void closeJump() {
        if (jumpPanel == null) return;
        root.removeView(jumpPanel);
        jumpPanel = null;
        for (FocusTile c : cards) c.setFocusable(true);
    }

    private FocusTile padKey(String label) {
        FocusTile b = new FocusTile(this, Tokens.RADIUS_CELL);
        b.setRest(Tokens.LAPIS_900, Tokens.LINE_SOFT, 2);
        TextView v = text(ui500, 28, Tokens.PLASTER_100);
        v.setText(t.d(label));
        v.setGravity(Gravity.CENTER);
        b.addView(v, new FrameLayout.LayoutParams(-1, -1));
        b.setLook(f -> v.setTextColor(f ? Tokens.LAPIS_900 : Tokens.PLASTER_100));
        return b;
    }

    /** Saves where reading stopped: the focused verse's own surah, verse, page and juz. */
    private void savePosition() {
        View f = getCurrentFocus();
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i) == f && ayahs != null && i < ayahs.length) {
                Quran.Ayah a = ayahs[i];
                if (a.id == lastSaved) return;
                lastSaved = a.id;
                final int surah = a.surah, ayah = a.ayah, page = a.page, juz = a.juz;
                Lib.io(() -> UserDb.of(this).saveReadPos(surah, ayah, page, juz), null);
                return;
            }
        }
    }

    // ---------------------------------------------------------------- keys

    @Override
    void onTick() {
        // The position follows the focused verse, written at most once a second.
        View f = getCurrentFocus();
        if (f instanceof FocusTile && cards.contains(f)) savePosition();
    }

    @Override
    protected void onPause() {
        savePosition();
        super.onPause();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (jumpPanel != null && keyCode == KeyEvent.KEYCODE_BACK) {
            closeJump();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // ---------------------------------------------------------------- helpers

    private LinearLayout.LayoutParams lp(int top, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = Ui.ipx(top);
        lp.bottomMargin = Ui.ipx(bottom);
        return lp;
    }

    private LinearLayout.LayoutParams chipLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, Ui.ipx(64 + 2 * FocusTile.REACH));
        lp.setMarginStart(Ui.ipx(20 - 2 * FocusTile.REACH));
        return lp;
    }

    private FocusTile headerChip(int iconKind, String label) {
        FocusTile b = new FocusTile(this, 34);
        b.setRest(Tokens.LAPIS_800, Tokens.LINE_SOFT, 2);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.ipx(24), 0, Ui.ipx(28), 0);
        Icon icon = new Icon(this, iconKind, 1.8f);
        TextView v = text(ui400, 22, Tokens.PLASTER_100);
        v.setText(label);
        v.setSingleLine(true);
        row.addView(icon, new LinearLayout.LayoutParams(Ui.ipx(26), Ui.ipx(26)));
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
}
