package pro.datascoop.tv;

import android.content.Intent;
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
 * The reading bookmarks: every marked verse, newest first, opening the reader at its place. OK removes a
 * bookmark; Back returns. An empty state explains what marking is for.
 */
public class BookmarksActivity extends Screen {

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
        title.setText(t.qrBookmarks());
        lineHeight(title, 1f);
        head.addView(title);
        TextView sub = text(ui400, 22, Tokens.MIST_400);
        sub.setText(t.qrBookmarks());
        head.addView(sub, lp(4, 0));
        head.setClipChildren(false);
        place(head, 96 + 72 + 28, 56, ViewGroup.LayoutParams.WRAP_CONTENT, 84);

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

        Lib.io(() -> {
            final List<int[]> marks = UserDb.of(this).bookmarks();
            Lib.main(() -> fill(marks));
        }, null);
    }

    private List<String> hints2() {
        List<String> h = new ArrayList<>();
        h.add(t.hintOk(t.qrBookmark()));
        h.add(t.hintBackHome());
        return h;
    }

    private void fill(List<int[]> marks) {
        if (isFinishing() || list == null) return;
        list.removeAllViews();
        if (marks.isEmpty()) {
            TextView none = text(ui400, 26, Tokens.MIST_400);
            none.setText(t.qiNoResults());
            none.setPadding(Ui.ipx(16), Ui.ipx(16), Ui.ipx(16), Ui.ipx(16));
            list.addView(none, lp(24, 24));
            return;
        }
        Quran.ready(this, () -> {
            Quran q = Quran.opened();
            if (q == null || isFinishing()) return;
            for (final int[] m : marks) {
                Quran.Surah s = q.surah(m[0]);
                Quran.Ayah[] loaded = null;
                FocusTile row = new FocusTile(this, Tokens.RADIUS_ROW);
                row.setRest(Tokens.LAPIS_800, Tokens.LINE_SOFT, 2);
                row.setFocusScale(Tokens.FOCUS_SCALE_ROW);
                LinearLayout col = new LinearLayout(this);
                col.setOrientation(LinearLayout.VERTICAL);
                col.setPadding(Ui.ipx(30), Ui.ipx(20), Ui.ipx(30), Ui.ipx(18));
                TextView where = text(ui500, 26, Tokens.GOLD_400);
                where.setText((t.ar ? s.name : s.nameEn) + t.sep() + t.qrAyahMark(m[1]));
                where.setTextDirection(View.TEXT_DIRECTION_RTL);
                TextView verse = text(sacred, 30, Tokens.PLASTER_100);
                verse.setLineSpacing(Ui.ipx(6), 1f);
                verse.setTextDirection(View.TEXT_DIRECTION_RTL);
                verse.setMaxLines(3);
                verse.setEllipsize(TextUtils.TruncateAt.END);
                verse.setText(verseText(q, m[0], m[1]));
                col.addView(where);
                col.addView(verse, lp(8, 0));
                row.addView(col, new FrameLayout.LayoutParams(-1, -1));
                row.setLook(f -> verse.setTextColor(f ? Tokens.LAPIS_900 : Tokens.PLASTER_100));
                row.setOnClickListener(v -> startSafely(new Intent(this, QuranReaderActivity.class)
                    .putExtra("surah", m[0]).putExtra("ayah", m[1])));
                list.addView(row, lp(0, 12));
            }
            if (list.getChildCount() > 0 && list.getChildAt(0) instanceof FocusTile) {
                list.getChildAt(0).requestFocus();
            }
        });
    }

    /** The marked verse's own text (a one-verse query, cached in the surah rows it was read from). */
    private String verseText(Quran q, int surah, int ayah) {
        for (Quran.Ayah a : q.surahAyahs(surah)) if (a.ayah == ayah) return a.text;
        return "";
    }

    @Override
    void onTick() {
    }

    private LinearLayout.LayoutParams lp(int top, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = Ui.ipx(top);
        lp.bottomMargin = Ui.ipx(bottom);
        return lp;
    }
}
