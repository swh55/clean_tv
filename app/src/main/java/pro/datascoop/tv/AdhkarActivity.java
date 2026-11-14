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
 * One adhkar section of Hisn al-Muslim: the whole list (OK counts the focused zikr, one tap a repetition),
 * or one zikr at a time with a large counter (◀▶ move between zikrs). Each card carries its own count —
 * only where the book prints one — its source and its note, verbatim; Quranic verses inside a zikr are
 * coloured gold and set in Amiri, the way the Qur'an is shown elsewhere in the launcher.
 *
 * <p>The day's progress is saved with each count and is what the card shows next time; the reset chip
 * zeroes the section's counters (and only today's — old days go on their own).
 */
public class AdhkarActivity extends Screen {

    private Azkar.Zikr[] zikrs;
    private String sectionKey;
    private int[] progress;
    private boolean single;
    private ScrollView listScroll;
    private LinearLayout list;
    private LinearLayout singleCard;
    private TextView singleText, singleCounter, singleSource, singleNote, singleProgressLine;
    private View singleBar;
    private LinearLayout singleBarTrack;
    private FocusTile prevBtn, nextBtn, modeChip, resetChip;
    private TextView countChipLabel;
    private LinearLayout hints;
    private int at;               // the focused zikr in single mode

    @Override
    void build() {
        newRoot();
        int section = getIntent().getIntExtra("section", Azkar.MORNING);
        zikrs = Azkar.get(this).section(section);
        sectionKey = section == Azkar.MORNING ? "m" : section == Azkar.EVENING ? "e" : "p";
        single = Prefs.adhkarSingle(this);
        at = 0;

        // ---- header: back, the section's name, the progress chip, the mode and reset chips
        FocusTile back = new FocusTile(this, 36);
        back.setRest(Tokens.LAPIS_800, Tokens.LINE_SOFT, 2);
        Icon backIcon = new Icon(this, Icon.BACK, 2f);
        back.addView(backIcon, new FrameLayout.LayoutParams(Ui.ipx(32), Ui.ipx(32), Gravity.CENTER));
        back.setLook(f -> backIcon.setColor(f ? Tokens.LAPIS_900 : Tokens.PLASTER_100));
        back.setContentDescription(t.back());
        back.setOnClickListener(v -> finish());
        placeTile(back, 96, 56, 72, 72);

        String title = section == Azkar.MORNING ? t.libMorning() : section == Azkar.EVENING ? t.libEvening() : t.libAfter();
        TextView name = text(displayBold, 56, Tokens.PLASTER_100);
        name.setText(title);
        lineHeight(name, 1f);
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        head.addView(name);
        countChipLabel = text(ui400, 22, Tokens.MIST_400);
        head.addView(countChipLabel, lp(4, 0));
        head.setClipChildren(false);
        place(head, 96 + 72 + 28, 56, ViewGroup.LayoutParams.WRAP_CONTENT, 84);

        modeChip = chip(Icon.BOOK, single ? t.adhkarModeSingle() : t.adhkarModeList());
        modeChip.setOnClickListener(v -> {
            Prefs.setAdhkarSingle(this, single = !single);
            build();
        });
        resetChip = chip(Icon.CHECK, t.adhkarReset());
        resetChip.setOnClickListener(v -> reset());
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setClipChildren(false);
        float r = FocusTile.REACH;
        chips.addView(modeChip, new LinearLayout.LayoutParams(-2, Ui.ipx(64 + 2 * r)));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-2, Ui.ipx(64 + 2 * r));
        rlp.setMarginStart(Ui.ipx(20 - 2 * r));
        chips.addView(resetChip, rlp);
        place(chips, 1920 - 96 - 560, 56, 560, 64 + 2 * r);

        hints = new LinearLayout(this);
        hints.setOrientation(LinearLayout.HORIZONTAL);
        place(hints, 96, 962, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // ---- body
        if (single) buildSingle();
        else buildList();
        renderProgress();
        hint();
    }

    @Override
    void onTick() {
    }

    // ---------------------------------------------------------------- list mode

    private void buildList() {
        listScroll = new ScrollView(this);
        listScroll.setFillViewport(true);
        listScroll.setVerticalScrollBarEnabled(false);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(Ui.ipx(24), Ui.ipx(4), Ui.ipx(24), Ui.ipx(60));
        list.setClipChildren(false);
        listScroll.addView(list, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        place(listScroll, 96, 200, 1728, 760);

        Azkar az = Azkar.get(this);
        if (!az.intro().isEmpty() && "m".equals(sectionKey)) {
            TextView intro = text(ui400, 22, Tokens.MIST_400);
            intro.setText(az.intro());
            intro.setPadding(Ui.ipx(8), Ui.ipx(8), Ui.ipx(8), Ui.ipx(8));
            list.addView(intro, lp(24, 12));
        }
        for (int i = 0; i < zikrs.length; i++) list.addView(zikrCard(i), lp(0, 16));
        // focus the first card once laid out
        list.post(() -> {
            View first = list.getChildAt(zikrOffset());
            if (first != null) first.requestFocus();
        });
    }

    /** The intro (when shown) sits before the cards. */
    private int zikrOffset() {
        return list.getChildCount() > zikrs.length ? 1 : 0;
    }

    /** One zikr: its counted text, its repetition, its progress and its source. */
    private View zikrCard(int i) {
        Azkar.Zikr z = zikrs[i];
        FocusTile tile = new FocusTile(this, Tokens.RADIUS_ROW);
        tile.setRest(Tokens.LAPIS_800, Tokens.LINE_SOFT, 2);
        tile.setFocusScale(Tokens.FOCUS_SCALE_ROW);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.ipx(30), Ui.ipx(24), Ui.ipx(30), Ui.ipx(22));
        col.setClipChildren(false);

        TextView body = text(sacred, 32, Tokens.PLASTER_100);
        body.setText(zikrText(z));
        body.setLineSpacing(Ui.ipx(6), 1f);
        body.setTextDirection(View.TEXT_DIRECTION_RTL);
        col.addView(body);

        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.HORIZONTAL);
        meta.setGravity(Gravity.CENTER_VERTICAL);
        TextView times = text(ui600, 22, Tokens.GOLD_400);
        times.setText(t.adhkarTimes(z.count));
        TextView state = text(ui500, 24, Tokens.GOLD_400);
        meta.addView(times);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-2, -2);
        slp.setMarginStart(Ui.ipx(16));
        meta.addView(state, slp);
        View space = new View(this);
        meta.addView(space, new LinearLayout.LayoutParams(0, 1, 1));
        TextView src = text(ui400, 20, Tokens.MIST_400);
        src.setMaxLines(2);
        src.setEllipsize(TextUtils.TruncateAt.END);
        src.setText(t.adhkarSource() + t.sep() + z.source);
        meta.addView(src, new LinearLayout.LayoutParams(0, -2, 1.6f));
        col.addView(meta, lp(14, 0));

        if (!z.note.isEmpty()) {
            TextView note = text(ui400, 20, Tokens.MIST_400);
            note.setMaxLines(3);
            note.setEllipsize(TextUtils.TruncateAt.END);
            note.setText(t.adhkarNote() + t.sep() + z.note);
            col.addView(note, lp(8, 0));
        }
        tile.addView(col, new FrameLayout.LayoutParams(-1, -1));
        tile.setLook(f -> body.setTextColor(f ? Tokens.LAPIS_900 : Tokens.PLASTER_100));
        final int idx = i;
        tile.setOnClickListener(v -> count(idx));
        tile.setTag(new Object[] {state, tile});
        return tile;
    }

    // ---------------------------------------------------------------- single mode

    private void buildSingle() {
        singleCard = new LinearLayout(this);
        singleCard.setOrientation(LinearLayout.VERTICAL);
        singleCard.setPadding(Ui.ipx(48), Ui.ipx(40), Ui.ipx(48), Ui.ipx(36));
        singleCard.setClipChildren(false);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Tokens.LAPIS_800);
        bg.setCornerRadius(Ui.px(Tokens.RADIUS_PANEL));
        bg.setStroke(Ui.ipx(2), Tokens.GOLD_400_60);
        singleCard.setBackground(bg);

        singleText = text(sacred, 40, Tokens.PLASTER_100);
        singleText.setTextDirection(View.TEXT_DIRECTION_RTL);
        singleText.setLineSpacing(Ui.ipx(8), 1f);
        singleCard.addView(singleText);

        singleProgressLine = text(ui400, 22, Tokens.MIST_400);
        singleCard.addView(singleProgressLine, lp(24, 0));

        LinearLayout bar = new LinearLayout(this);
        singleBarTrack = new LinearLayout(this);
        singleBarTrack.setOrientation(LinearLayout.HORIZONTAL);
        singleBar = new View(this);
        singleBar.setBackgroundColor(Tokens.GOLD_400);
        singleBarTrack.addView(singleBar, new LinearLayout.LayoutParams(0, -1, 0));
        singleBarTrack.setBackgroundColor(Tokens.BANNER_TRACK);
        bar.addView(singleBarTrack, new LinearLayout.LayoutParams(-1, Ui.ipx(8)));
        singleCard.addView(bar, lp(0, 0));

        singleCounter = text(display, 88, Tokens.GOLD_400);
        singleCounter.setTextDirection(View.TEXT_DIRECTION_RTL);
        singleCounter.setGravity(Gravity.CENTER);
        lineHeight(singleCounter, 1f);
        singleCard.addView(singleCounter, lp(16, 0));

        singleSource = text(ui400, 22, Tokens.MIST_400);
        singleCard.addView(singleSource, lp(12, 0));
        singleNote = text(ui400, 20, Tokens.MIST_400);
        singleCard.addView(singleNote, lp(8, 0));

        place(singleCard, 240, 200, 1440, 560);

        prevBtn = singleButton(Icon.PREV, t.plPrev());
        prevBtn.setOnClickListener(v -> move(-1));
        nextBtn = singleButton(Icon.NEXT, t.plNext());
        nextBtn.setOnClickListener(v -> move(1));
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER_HORIZONTAL);
        nav.setClipChildren(false);
        float r = FocusTile.REACH;
        nav.addView(prevBtn, new LinearLayout.LayoutParams(-2, Ui.ipx(68 + 2 * r)));
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(-2, Ui.ipx(68 + 2 * r));
        nlp.setMarginStart(Ui.ipx(32));
        nav.addView(nextBtn, nlp);
        place(nav, 0, 796, 1920, 68 + 2 * r);
        showZikr(at);
        prevBtn.requestFocus();
    }

    private FocusTile singleButton(int iconKind, String label) {
        FocusTile b = new FocusTile(this, 34);
        b.setRest(Tokens.LAPIS_900, Tokens.LINE_SOFT, 2);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.ipx(28), 0, Ui.ipx(32), 0);
        Icon icon = new Icon(this, iconKind, 2f);
        TextView v = text(ui400, 24, Tokens.PLASTER_100);
        v.setText(label);
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

    private void showZikr(int i) {
        at = Math.max(0, Math.min(zikrs.length - 1, i));
        Azkar.Zikr z = zikrs[at];
        singleText.setText(zikrText(z));
        singleSource.setText(t.adhkarSource() + t.sep() + z.source);
        singleNote.setText(z.note.isEmpty() ? "" : t.adhkarNote() + t.sep() + z.note);
        singleNote.setVisibility(z.note.isEmpty() ? View.GONE : View.VISIBLE);
        singleProgressLine.setText(t.adhkarProgress(at + 1, zikrs.length) + t.sep() + t.adhkarTimes(z.count));
        renderProgress();
    }

    private void move(int step) {
        showZikr(at + step);
    }

    // ---------------------------------------------------------------- counting

    /** One repetition of the zikr at i (the focused one), saved as the day's progress. */
    private void count(int i) {
        Azkar.Zikr z = zikrs[i];
        int now = progress == null ? 0 : progress[i];
        if (now >= z.count) return;   // done for today: OK does nothing further
        int next = now + 1;
        final int idx = i;
        Lib.io(() -> UserDb.of(this).azkarAdd(sectionKey, UserDb.today(), idx, next), null);
        progress[i] = next;
        renderProgress();
        if (single && i == at) showZikr(at);
    }

    private void reset() {
        Lib.io(() -> UserDb.of(this).azkarReset(sectionKey), this::renderProgress);
    }

    /** Every zikr's card, the header chip and the single view, in the day's state. */
    private void renderProgress() {
        Lib.io(() -> {
            final int[] p = UserDb.of(this).azkarProgress(sectionKey, UserDb.today(), zikrs.length);
            Lib.main(() -> {
                progress = p;
                int done = 0;
                for (int c : p) if (c > 0) done++;
                countChipLabel.setText(t.adhkarCount(zikrs.length) + t.sep() + t.adhkarProgress(done, zikrs.length));
                if (!single && list != null) {
                    for (int i = 0; i < zikrs.length; i++) {
                        View child = list.getChildAt(i + zikrOffset());
                        if (!(child instanceof FocusTile)) continue;
                        Object[] tag = (Object[]) child.getTag();
                        TextView state = (TextView) tag[0];
                        FocusTile tile = (FocusTile) tag[1];
                        state.setText(p[i] >= zikrs[i].count ? t.adhkarDoneAll() : t.adhkarXOf(p[i], zikrs[i].count));
                        tile.setRest(Tokens.LAPIS_800, p[i] >= zikrs[i].count ? Tokens.GOLD_400 : Tokens.LINE_SOFT, 2);
                    }
                } else if (single && singleCounter != null) {
                    Azkar.Zikr z = zikrs[at];
                    int c = p[at];
                    singleCounter.setText(c >= z.count ? t.adhkarDoneAll() : t.adhkarXOf(c, z.count));
                    float w = z.count <= 0 ? 0 : Math.min(1f, c / (float) z.count);
                    singleBar.getLayoutParams().width = Math.round(Ui.ipx(1440 - 96) * w);
                    singleBar.requestLayout();
                }
            });
        });
    }

    // ---------------------------------------------------------------- text styling

    /** The zikr with its Quranic {…} pieces in gold and its own typeface, plain words in plaster. */
    private CharSequence zikrText(Azkar.Zikr z) {
        List<Object[]> segs = Quran.segments(z.text);
        SpannableString s = new SpannableString(z.text);
        int at = 0;
        for (Object[] seg : segs) {
            String piece = (String) seg[0];
            boolean quran = (Boolean) seg[1];
            if (quran) s.setSpan(new ForegroundColorSpan(Tokens.GOLD_400), at, at + piece.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            at += piece.length();
        }
        return s;
    }

    // ---------------------------------------------------------------- chrome

    private FocusTile chip(int iconKind, String label) {
        FocusTile b = new FocusTile(this, 36);
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
        final TextView fv = v;
        final Icon fi = icon;
        b.setLook(f -> {
            int c = f ? Tokens.LAPIS_900 : Tokens.PLASTER_100;
            fv.setTextColor(c);
            fi.setColor(c);
        });
        return b;
    }

    private LinearLayout.LayoutParams lp(int top, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = Ui.ipx(top);
        lp.bottomMargin = Ui.ipx(bottom);
        return lp;
    }

    private void hint() {
        List<String> h = new ArrayList<>();
        h.add(single ? t.hintAdhkarSingle() : t.hintAdhkarList());
        h.add(t.hintBackHome());
        setHints(hints, h);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int k = e.getKeyCode();
        if (single && e.getAction() == KeyEvent.ACTION_DOWN) {
            boolean left = k == KeyEvent.KEYCODE_DPAD_LEFT, right = k == KeyEvent.KEYCODE_DPAD_RIGHT;
            if (left || right) {
                // In Arabic the arrows mirror, so "forward" is toward the reading direction's end.
                move((right != rtl) ? 1 : -1);
                return true;
            }
            if (k == KeyEvent.KEYCODE_DPAD_UP) {
                prevBtn.requestFocus();
                return true;
            }
            if (k == KeyEvent.KEYCODE_DPAD_DOWN) {
                nextBtn.requestFocus();
                return true;
            }
        }
        return super.dispatchKeyEvent(e);
    }
}
