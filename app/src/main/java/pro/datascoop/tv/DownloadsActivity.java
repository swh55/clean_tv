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
 * The download manager: what is downloaded of each reciter (a row per chapter: the surah's name, the file's
 * size, a delete button — a new row for downloading one more chapter). Nothing downloads on its own: the
 * button asks for a surah number with a small digit pad and hands it to the system's DownloadManager; the
 * screen settles the registry on every visit, so a chapter finished while the TV was off is marked right.
 */
public class DownloadsActivity extends Screen {

    private LinearLayout list;
    private LinearLayout hints;
    private TextView total;

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
        title.setText(t.dlTitle());
        lineHeight(title, 1f);
        head.addView(title);
        total = text(ui400, 22, Tokens.MIST_400);
        head.addView(total, lp(4, 0));
        head.setClipChildren(false);
        place(head, 96 + 72 + 28, 56, 700, 84);

        FocusTile add = chip(Icon.DOWNLOAD, t.plDownload());
        add.setOnClickListener(v -> openAdd());
        placeEnd(add, 96, 56, ViewGroup.LayoutParams.WRAP_CONTENT, 72);

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
        h.add(t.hintOk(t.dlTitle()));
        h.add(t.hintBackHome());
        return h;
    }

    /** Settles finished downloads, then lists every downloaded chapter with its size and delete. */
    private void refresh() {
        final DownloadsActivity self = this;
        Lib.io(() -> {
            Downloads.settle(this);
            final List<Object[]> rows = new ArrayList<>();
            long bytes = 0;
            int files = 0;
            for (Reciters.Reciter r : Reciters.get(this).all()) {
                for (int[] d : UserDb.of(this).downloads(r.id)) {
                    rows.add(new Object[] {r, d[0], (long) d[1], d[2]});
                    if (d[2] == UserDb.DL_DONE) {
                        bytes += d[1];
                        files++;
                    }
                }
            }
            final long size = bytes;
            final int count = files;
            Lib.main(() -> {
                if (self.isFinishing() || list == null) return;
                total.setText(count == 0 ? t.dlNone() : t.rcDownloads(count) + t.sep() + Lib.bytes(t, size));
                list.removeAllViews();
                if (rows.isEmpty()) {
                    TextView none = text(ui400, 26, Tokens.MIST_400);
                    none.setText(t.dlNone());
                    none.setPadding(Ui.ipx(16), Ui.ipx(16), Ui.ipx(16), Ui.ipx(16));
                    list.addView(none, lp(24, 24));
                    return;
                }
                for (Object[] row : rows) list.addView(downloadRow((Reciters.Reciter) row[0], (int) row[1], (long) row[2], (int) row[3]), lp(0, 12));
                if (list.getChildCount() > 0 && list.getChildAt(0) instanceof FocusTile) list.getChildAt(0).requestFocus();
            });
        }, null);
    }

    /** One downloaded chapter: the surah and its size, OK plays it, the delete pill drops the file. */
    private View downloadRow(Reciters.Reciter r, int surah, long bytes, int status) {
        FocusTile tile = new FocusTile(this, Tokens.RADIUS_ROW);
        tile.setRest(Tokens.LAPIS_800, status == UserDb.DL_DONE ? Tokens.LINE_SOFT : Tokens.GOLD_400_60, 2);
        tile.setFocusScale(Tokens.FOCUS_SCALE_ROW);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.ipx(30), Ui.ipx(18), Ui.ipx(30), Ui.ipx(16));
        TextView name = text(ui500, 26, Tokens.PLASTER_100);
        Quran.ready(this, () -> {
            Quran q = Quran.opened();
            if (q != null && name != null) {
                Quran.Surah s = q.surah(surah);
                name.setText(r.name(t) + t.sep() + (t.ar ? s.name : s.nameEn));
            }
        });
        col.addView(name);
        TextView size = text(ui400, 20, Tokens.MIST_400);
        size.setText(status == UserDb.DL_DONE ? Lib.bytes(t, bytes)
            : status == UserDb.DL_RUNNING ? t.plDownloading((int) Math.floor(Downloads.progress(this, r.id, surah) * 100))
            : t.dlFailed());
        col.addView(size, lp(4, 0));
        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.HORIZONTAL);
        inner.setGravity(Gravity.CENTER_VERTICAL);
        inner.addView(col, new LinearLayout.LayoutParams(0, -2, 1f));
        tile.addView(inner, new FrameLayout.LayoutParams(-1, -2));
        tile.setLook(f -> name.setTextColor(f ? Tokens.LAPIS_900 : Tokens.PLASTER_100));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClipChildren(false);
        float rr = FocusTile.REACH;
        FocusTile play = small(Icon.PLAY);
        play.setOnClickListener(v -> startSafely(new Intent(this, PlayerActivity.class).putExtra("surah", surah)));
        FocusTile del = small(Icon.STOP);
        del.setOnClickListener(v -> {
            Downloads.delete(this, r.id, surah);
            refresh();
        });
        row.addView(play, new LinearLayout.LayoutParams(Ui.ipx(60 + 2 * rr), Ui.ipx(60 + 2 * rr)));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(Ui.ipx(60 + 2 * rr), Ui.ipx(60 + 2 * rr));
        dlp.setMarginStart(Ui.ipx(12 - 2 * rr));
        row.addView(del, dlp);

        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.HORIZONTAL);
        wrap.setClipChildren(false);
        wrap.addView(tile, new LinearLayout.LayoutParams(0, -1, 1f));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-2, -2);
        rlp.setMarginStart(Ui.ipx(12));
        wrap.addView(row, rlp);
        return wrap;
    }

    /** Asks for a surah number, then hands the chapter to the system's downloader. */
    private void openAdd() {
        final String reciter = Prefs.reciter(this);
        final StringBuilder buf = new StringBuilder();
        FrameLayout panel = new FrameLayout(this);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(Tokens.LAPIS_700);
        bg.setCornerRadius(Ui.px(Tokens.RADIUS_PANEL));
        bg.setStroke(Ui.ipx(2), Tokens.GOLD_400_60);
        panel.setBackground(bg);
        panel.setPadding(Ui.ipx(36), Ui.ipx(30), Ui.ipx(36), Ui.ipx(30));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setClipChildren(false);
        TextView title = text(displayBold, 40, Tokens.PLASTER_100);
        title.setText(t.plDownload());
        col.addView(title);
        final TextView shown = text(display, 64, Tokens.GOLD_400);
        shown.setTextDirection(View.TEXT_DIRECTION_LTR);
        col.addView(shown, lp(8, 0));
        LinearLayout pad = new LinearLayout(this);
        pad.setOrientation(LinearLayout.VERTICAL);
        col.addView(pad, lp(16, 0));
        final List<FocusTile> keys = new ArrayList<>();
        LinearLayout rowA = new LinearLayout(this);
        rowA.setClipChildren(false);
        LinearLayout rowB = new LinearLayout(this);
        rowB.setClipChildren(false);
        int i = 0;
        for (char d : "1234567890⌫".toCharArray()) {
            FocusTile key = keyTile(String.valueOf(d));
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
            LinearLayout.LayoutParams klp = new LinearLayout.LayoutParams(Ui.ipx(96), Ui.ipx(60));
            if (i % 6 > 0) klp.setMarginStart(Ui.ipx(14));
            (i < 6 ? rowA : rowB).addView(key, klp);
            i++;
        }
        pad.addView(rowA, lp(0, 0));
        pad.addView(rowB, lp(8, 0));
        FocusTile go = new FocusTile(this, 34);
        go.setRest(Tokens.LAPIS_900, Tokens.GOLD_400_60, 2);
        TextView goLabel = text(ui500, 26, Tokens.PLASTER_100);
        goLabel.setText(t.plDownload());
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
            closeAdd();
            if (n >= 1 && n <= Quran.SURAHS && Downloads.file(this, reciter, n) == null
                && !Downloads.running(this, reciter, n)) {
                Downloads.start(this, reciter, n);
                refresh();
            }
        });
        col.addView(go, lp(24, 0));
        panel.addView(col, new FrameLayout.LayoutParams(-1, -1));
        FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(Ui.ipx(640), -2, Gravity.TOP | Gravity.END);
        plp.setMarginEnd(Ui.ipx(96));
        plp.topMargin = Ui.ipx(160);
        root.addView(panel, plp);
        addPanel = panel;
        addTiles = keys;
        if (!keys.isEmpty()) keys.get(0).requestFocus();
    }

    private FrameLayout addPanel;
    private List<FocusTile> addTiles;

    private void closeAdd() {
        if (addPanel == null) return;
        root.removeView(addPanel);
        addPanel = null;
    }

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if (addPanel != null && keyCode == android.view.KeyEvent.KEYCODE_BACK) {
            closeAdd();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    void onTick() {
        // a download finishing while the screen is open shows up on the next second
        if (addPanel == null && tickCount++ % 5 == 0) refresh();
    }

    private int tickCount;

    // ---------------------------------------------------------------- chrome

    private FocusTile keyTile(String label) {
        FocusTile b = new FocusTile(this, Tokens.RADIUS_CELL);
        b.setRest(Tokens.LAPIS_900, Tokens.LINE_SOFT, 2);
        TextView v = text(ui500, 28, Tokens.PLASTER_100);
        v.setText(t.d(label));
        v.setGravity(Gravity.CENTER);
        b.addView(v, new FrameLayout.LayoutParams(-1, -1));
        b.setLook(f -> v.setTextColor(f ? Tokens.LAPIS_900 : Tokens.PLASTER_100));
        return b;
    }

    private FocusTile small(int iconKind) {
        FocusTile b = new FocusTile(this, 999);
        b.setRest(Tokens.LAPIS_700, Tokens.LINE_SOFT, 2);
        Icon icon = new Icon(this, iconKind, 1.8f);
        b.addView(icon, new FrameLayout.LayoutParams(Ui.ipx(28), Ui.ipx(28), Gravity.CENTER));
        b.setLook(f -> icon.setColor(f ? Tokens.LAPIS_900 : Tokens.PLASTER_100));
        return b;
    }

    private FocusTile chip(int iconKind, String label) {
        FocusTile b = new FocusTile(this, 34);
        b.setRest(Tokens.LAPIS_800, Tokens.LINE_SOFT, 2);
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

    private LinearLayout.LayoutParams lp(int top, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = Ui.ipx(top);
        lp.bottomMargin = Ui.ipx(bottom);
        return lp;
    }
}
